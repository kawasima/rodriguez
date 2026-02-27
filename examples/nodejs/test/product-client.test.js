import { describe, test, expect } from 'vitest';
import { ProductClient, ProductApiError } from '../src/product-client.js';

const PORTS = {
  REFUSE_CONNECTION:     10201,
  NOT_ACCEPT:            10202,
  RST_AFTER_DELAY:       10203,
  NEVER_DRAIN:           10204,
  SLOW_RESPONSE:         10205,
  CONTENT_TYPE_MISMATCH: 10206,
  RESPONSE_HEADER_ONLY:  10207,
  BROKEN_JSON:           10208,
  ACCEPT_BUT_SILENT:     10209,
  OVERSIZED_RESPONSE:    10211,
  REFUSE_AUTH:           10212,
};

function clientFor(port, options = {}) {
  return new ProductClient(`http://localhost:${port}`, {
    timeoutMs: options.timeoutMs ?? 2000,
    maxResponseBytes: options.maxResponseBytes,
  });
}

async function getError(client) {
  try {
    await client.getProduct('1');
    throw new Error('Expected an error but call succeeded');
  } catch (err) {
    if (err instanceof ProductApiError) return err;
    throw err;
  }
}

// --- TCP-level faults ---

describe('RefuseConnection (port 10201)', () => {
  test('should throw CONNECTION_ERROR when server refuses connection', async () => {
    const client = clientFor(PORTS.REFUSE_CONNECTION, { timeoutMs: 1000 });
    const err = await getError(client);
    expect(err.code).toBe('CONNECTION_ERROR');
  });
});

describe('NotAccept (port 10202)', () => {
  test('should throw TIMEOUT or CONNECTION_ERROR when server never accepts', async () => {
    const client = clientFor(PORTS.NOT_ACCEPT, { timeoutMs: 2000 });
    const err = await getError(client);
    expect(['TIMEOUT', 'CONNECTION_ERROR']).toContain(err.code);
  });
});

describe('NoResponseAndSendRST (port 10203)', () => {
  test('should throw CONNECTION_ERROR when server sends RST', async () => {
    const client = clientFor(PORTS.RST_AFTER_DELAY, { timeoutMs: 5000 });
    const err = await getError(client);
    expect(err.code).toBe('CONNECTION_ERROR');
  });
});

describe('NeverDrain (port 10204)', () => {
  test('should throw TIMEOUT when server never processes data', async () => {
    const client = clientFor(PORTS.NEVER_DRAIN, { timeoutMs: 2000 });
    const err = await getError(client);
    expect(['TIMEOUT', 'CONNECTION_ERROR']).toContain(err.code);
  });
});

describe('AcceptButSilent (port 10209)', () => {
  test('should throw TIMEOUT when server accepts but never responds', async () => {
    const client = clientFor(PORTS.ACCEPT_BUT_SILENT, { timeoutMs: 2000 });
    const err = await getError(client);
    expect(err.code).toBe('TIMEOUT');
  });
});

// --- HTTP-level faults ---

describe('SlowResponse (port 10205)', () => {
  test('should throw when reading body takes too long', async () => {
    const client = clientFor(PORTS.SLOW_RESPONSE, { timeoutMs: 2000 });
    const err = await getError(client);
    expect(['TIMEOUT', 'BODY_READ_ERROR']).toContain(err.code);
  });
});

describe('ContentTypeMismatch (port 10206)', () => {
  test('should detect HTTP 400 error', async () => {
    const client = clientFor(PORTS.CONTENT_TYPE_MISMATCH, { timeoutMs: 3000 });
    const err = await getError(client);
    expect(err.code).toBe('HTTP_ERROR');
    expect(err.status).toBe(400);
  });
});

describe('ResponseHeaderOnly (port 10207)', () => {
  test('should throw when response body never arrives completely', async () => {
    const client = clientFor(PORTS.RESPONSE_HEADER_ONLY, { timeoutMs: 3000 });
    const err = await getError(client);
    expect(['TIMEOUT', 'BODY_READ_ERROR']).toContain(err.code);
  });
});

describe('BrokenJson (port 10208)', () => {
  test('should detect broken JSON in response body', async () => {
    const client = clientFor(PORTS.BROKEN_JSON, { timeoutMs: 3000 });
    const err = await getError(client);
    expect(err.code).toBe('INVALID_JSON');
  });
});

describe('OversizedResponse (port 10211)', () => {
  test('should reject response that exceeds size limit', async () => {
    const client = clientFor(PORTS.OVERSIZED_RESPONSE, {
      timeoutMs: 10000,
      maxResponseBytes: 1024 * 1024,
    });
    const err = await getError(client);
    expect(err.code).toBe('RESPONSE_TOO_LARGE');
  }, 15000);
});

describe('RefuseAuthentication (port 10212)', () => {
  test('should detect 401 Unauthorized', async () => {
    const client = clientFor(PORTS.REFUSE_AUTH, { timeoutMs: 3000 });
    const err = await getError(client);
    expect(err.code).toBe('UNAUTHORIZED');
    expect(err.status).toBe(401);
  });
});
