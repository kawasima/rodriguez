import { describe, test, expect, beforeAll, afterAll } from 'vitest';
import { Storage } from '@google-cloud/storage';

const GCS_PORT = 10215;
const BUCKET = 'vitest-gcs-bucket';
const FILE_NAME = 'hello.txt';
const BODY = 'Hello from vitest GCS test';

const FAULT_PORTS = {
  SLOW_RESPONSE:        10205,
  RESPONSE_HEADER_ONLY: 10207,
  ACCEPT_BUT_SILENT:    10209,
};

function createGCSClient(port = GCS_PORT, options = {}) {
  return new Storage({
    apiEndpoint: `http://localhost:${port}`,
    projectId: 'test-project',
    retryOptions: { maxRetries: 0 },
    ...options,
  });
}

// --- Normal operation ---

describe('GCS Mock (port 10215)', () => {
  let storage;

  beforeAll(() => {
    storage = createGCSClient();
  });

  afterAll(async () => {
    try {
      await storage.bucket(BUCKET).file(FILE_NAME).delete();
      await storage.bucket(BUCKET).delete();
    } catch (_) {
      // cleanup best-effort
    }
  });

  test('createBucket should succeed', async () => {
    const [bucket] = await storage.createBucket(BUCKET);
    expect(bucket.name).toBe(BUCKET);
  });

  test('file.save should upload an object', async () => {
    // resumable: false uses multipart upload (single POST)
    // The mock returns md5Hash/crc32c for integrity validation
    await storage.bucket(BUCKET).file(FILE_NAME).save(BODY, {
      resumable: false,
    });
  });

  test('getBuckets should include the created bucket', async () => {
    const [buckets] = await storage.getBuckets();
    expect(buckets).toBeDefined();
    const names = buckets.map(b => b.name);
    expect(names).toContain(BUCKET);
  });

  test('getFiles should include the uploaded object', async () => {
    const [files] = await storage.bucket(BUCKET).getFiles();
    expect(files).toBeDefined();
    const names = files.map(f => f.name);
    expect(names).toContain(FILE_NAME);
  });

  test('download should return the object content', async () => {
    const [contents] = await storage.bucket(BUCKET).file(FILE_NAME).download();
    expect(contents.toString()).toBe(BODY);
  });

  test('file.delete should succeed', async () => {
    await storage.bucket(BUCKET).file(FILE_NAME).delete();
  });

  test('bucket.delete should succeed', async () => {
    await storage.bucket(BUCKET).delete();
  });
});

// --- Fault injection: demonstrating the default timeout problem ---
//
// @google-cloud/storage uses gaxios (Axios-based) HTTP client internally.
// By default, there is no request timeout — requests to misbehaving servers
// will hang indefinitely.
//
// The tests below demonstrate:
//   1. Default config HANGS (detected by external safety net)
//   2. Properly configured timeout throws promptly

describe('GCS + AcceptButSilent (port 10209)', () => {
  test('PITFALL: default client hangs — no built-in timeout', async () => {
    const gcs = createGCSClient(FAULT_PORTS.ACCEPT_BUT_SILENT);
    const start = Date.now();
    await expect(
      Promise.race([
        gcs.createBucket('hang-test'),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('External timeout')), 3000)),
      ])
    ).rejects.toThrow();
    const elapsed = Date.now() - start;
    expect(elapsed).toBeGreaterThanOrEqual(2500);
  });

  test('FIX: application-level timeout via Promise.race', async () => {
    // @google-cloud/storage does not expose a per-request timeout option.
    // The only reliable workaround is application-level timeout via Promise.race.
    const gcs = createGCSClient(FAULT_PORTS.ACCEPT_BUT_SILENT);
    const start = Date.now();
    await expect(
      Promise.race([
        gcs.createBucket('timeout-test'),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Application timeout')), 1000)),
      ])
    ).rejects.toThrow(/timeout/i);
    const elapsed = Date.now() - start;
    expect(elapsed).toBeLessThan(2500);
  });
});

describe('GCS + SlowResponse (port 10205) — body stream hang', () => {
  test('PITFALL: download hangs on slow body — default has no timeout', async () => {
    // SlowResponse returns HTTP 200 immediately, then trickles body at 1 byte/sec.
    // The download will hang waiting for the full body to arrive.
    const gcs = createGCSClient(FAULT_PORTS.SLOW_RESPONSE);
    const start = Date.now();
    await expect(
      Promise.race([
        gcs.bucket('any').file('any').download(),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Body read timeout')), 2000)),
      ])
    ).rejects.toThrow(/timeout/i);
    const elapsed = Date.now() - start;
    expect(elapsed).toBeLessThan(3000);
  }, 10000);
});

describe('GCS + ResponseHeaderOnly (port 10207) — body stream hang', () => {
  test('PITFALL: download hangs — same as SlowResponse', async () => {
    // ResponseHeaderOnly sends HTTP 200 headers + 1 byte, then hangs forever.
    const gcs = createGCSClient(FAULT_PORTS.RESPONSE_HEADER_ONLY);
    const start = Date.now();
    await expect(
      Promise.race([
        gcs.bucket('any').file('any').download(),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Body read timeout')), 3000)),
      ])
    ).rejects.toThrow(/timeout/i);
    const elapsed = Date.now() - start;
    expect(elapsed).toBeLessThan(4000);
  }, 10000);
});
