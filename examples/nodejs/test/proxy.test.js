/**
 * Proxy-based fault injection scenario tests.
 *
 * Each test follows a realistic pattern: the application starts working
 * normally, then a fault is injected mid-flight, and we verify both the
 * failure behavior and the recovery.
 *
 * Setup (Docker):
 *   cd examples/nodejs
 *   docker compose up -d       # starts upstream + rodriguez with proxy
 *   npm test -- proxy.test.js  # run only this test file
 *   docker compose down        # cleanup
 *
 * Setup (local, no Docker):
 *   node examples/nodejs/src/upstream-server.js 8080                              # terminal 1
 *   mvn exec:java -pl rodriguez-build -Dexec.args="-c examples/nodejs/rodriguez-proxy.json"  # terminal 2
 *   cd examples/nodejs && npm test -- proxy.test.js                               # terminal 3
 */

import { describe, test, expect, afterEach } from 'vitest';
import { ProductClient, ProductApiError } from '../src/product-client.js';
import { CircuitBreaker, CircuitOpenError } from '../src/circuit-breaker.js';

const PROXY_PORT = 10220;
const PROXY_API = `http://localhost:${PROXY_PORT}/_proxy/api`;

const client = new ProductClient(`http://localhost:${PROXY_PORT}`, {
  timeoutMs: 3000,
});

// --- Proxy API helpers (used only by test setup/teardown) ---

async function injectFault({ pathPattern, faultType, count = 1, duration }) {
  const body = { pathPattern, faultType, count };
  if (duration) body.duration = duration;
  const res = await fetch(`${PROXY_API}/rules`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`Failed to inject fault: ${await res.text()}`);
  return res.json();
}

async function clearFaults() {
  await fetch(`${PROXY_API}/rules`, { method: 'DELETE' });
}

// --- Scenario tests ---

describe('Fault injection via proxy', () => {
  afterEach(async () => {
    await clearFaults();
  });

  test('transient failure: service works, breaks briefly, then recovers', async () => {
    // 1. Normal operation — requests succeed
    const product = await client.getProduct('42');
    expect(product.id).toBe(42);
    expect(product.name).toBe('Gadget');

    // 2. Inject a transient fault — next 2 requests return broken JSON
    await injectFault({
      pathPattern: '/products/.*',
      faultType: 'BrokenJson',
      count: 2,
    });

    // 3. During fault — application sees parse errors
    for (let i = 0; i < 2; i++) {
      await expect(client.getProduct('42')).rejects.toThrow(ProductApiError);
    }

    // 4. Recovery — fault rule exhausted, service is back to normal
    const recovered = await client.getProduct('42');
    expect(recovered.id).toBe(42);
  });

  test('auth outage: upstream auth service goes down and comes back', async () => {
    // 1. Normal — authenticated requests work
    const before = await client.getProduct('1');
    expect(before.name).toBe('Widget');

    // 2. Auth outage begins — next 3 requests get 401
    await injectFault({
      pathPattern: '/products/.*',
      faultType: 'RefuseAuthentication',
      count: 3,
    });

    // 3. Application should detect unauthorized errors
    for (let i = 0; i < 3; i++) {
      try {
        await client.getProduct('1');
        throw new Error('Expected UNAUTHORIZED');
      } catch (err) {
        expect(err).toBeInstanceOf(ProductApiError);
        expect(err.code).toBe('UNAUTHORIZED');
      }
    }

    // 4. Auth service recovers — requests succeed again
    const after = await client.getProduct('1');
    expect(after.name).toBe('Widget');
  });

  test('partial outage: one endpoint fails while another keeps working', async () => {
    // 1. Both endpoints work
    const product1 = await client.getProduct('1');
    const product42 = await client.getProduct('42');
    expect(product1.name).toBe('Widget');
    expect(product42.name).toBe('Gadget');

    // 2. Inject fault only on product 42's path
    await injectFault({
      pathPattern: '/products/42',
      faultType: 'BrokenJson',
      count: 5,
    });

    // 3. Product 1 is unaffected — still works fine
    const stillWorking = await client.getProduct('1');
    expect(stillWorking.name).toBe('Widget');

    // 4. Product 42 is broken
    await expect(client.getProduct('42')).rejects.toThrow(ProductApiError);

    // 5. After fault expires, product 42 also recovers
    // (consume the remaining 4 fault hits)
    for (let i = 0; i < 4; i++) {
      try { await client.getProduct('42'); } catch { /* expected */ }
    }
    const recovered42 = await client.getProduct('42');
    expect(recovered42.name).toBe('Gadget');
  });

  test('time-bounded outage: fault auto-resolves after duration', async () => {
    // 1. Normal operation
    const before = await client.getProduct('1');
    expect(before.name).toBe('Widget');

    // 2. Inject fault with 1-second TTL
    await injectFault({
      pathPattern: '/products/.*',
      faultType: 'BrokenJson',
      count: 1000,    // high count — won't expire by usage
      duration: '1s', // expires after 1 second regardless
    });

    // 3. During outage — requests fail
    await expect(client.getProduct('1')).rejects.toThrow(ProductApiError);

    // 4. Wait for TTL to expire
    await new Promise(r => setTimeout(r, 1500));

    // 5. Service auto-recovers without manual intervention
    const after = await client.getProduct('1');
    expect(after.name).toBe('Widget');
  });

  test('cascading failures: different fault types hit in sequence', async () => {
    // 1. Normal operation
    const before = await client.getProduct('42');
    expect(before.name).toBe('Gadget');

    // 2. First problem: upstream returns garbage JSON for 2 requests
    await injectFault({
      pathPattern: '/products/.*',
      faultType: 'BrokenJson',
      count: 2,
    });

    // 3. Then: upstream starts rejecting auth for 2 more requests
    await injectFault({
      pathPattern: '/products/.*',
      faultType: 'RefuseAuthentication',
      count: 2,
    });

    // 4. First wave — parse errors
    for (let i = 0; i < 2; i++) {
      try {
        await client.getProduct('42');
        throw new Error('Expected error');
      } catch (err) {
        expect(err).toBeInstanceOf(ProductApiError);
        expect(err.code).toBe('INVALID_JSON');
      }
    }

    // 5. Second wave — auth errors (different failure mode)
    for (let i = 0; i < 2; i++) {
      try {
        await client.getProduct('42');
        throw new Error('Expected error');
      } catch (err) {
        expect(err).toBeInstanceOf(ProductApiError);
        expect(err.code).toBe('UNAUTHORIZED');
      }
    }

    // 6. Full recovery
    const after = await client.getProduct('42');
    expect(after.name).toBe('Gadget');
  });

  test('circuit breaker: opens on failures, rejects fast, then recovers', async () => {
    // Wrap the client call in a circuit breaker:
    //   - opens after 3 consecutive failures
    //   - tries recovery probe after 1 second
    const breaker = new CircuitBreaker(
      (id) => client.getProduct(id),
      { failureThreshold: 3, resetTimeoutMs: 1000 },
    );

    // 1. Normal operation — circuit is CLOSED
    const product = await breaker.call('1');
    expect(product.name).toBe('Widget');
    expect(breaker.state).toBe('CLOSED');

    // 2. Inject sustained fault
    await injectFault({
      pathPattern: '/products/.*',
      faultType: 'BrokenJson',
      count: 10,
    });

    // 3. First 3 failures trip the circuit breaker to OPEN
    for (let i = 0; i < 3; i++) {
      await expect(breaker.call('1')).rejects.toThrow(ProductApiError);
    }
    expect(breaker.state).toBe('OPEN');

    // 4. While OPEN — requests are rejected immediately without hitting upstream
    await expect(breaker.call('1')).rejects.toThrow(CircuitOpenError);

    // 5. Clear the fault (simulate upstream recovery)
    await clearFaults();

    // 6. Wait for reset timeout — circuit transitions to HALF_OPEN
    await new Promise(r => setTimeout(r, 1200));

    // 7. HALF_OPEN probe succeeds → circuit closes
    const recovered = await breaker.call('1');
    expect(recovered.name).toBe('Widget');
    expect(breaker.state).toBe('CLOSED');
  });
});
