import { describe, test, expect, beforeAll, afterAll } from 'vitest';
import {
  S3Client,
  CreateBucketCommand,
  PutObjectCommand,
  ListBucketsCommand,
  ListObjectsCommand,
  GetObjectCommand,
  DeleteObjectCommand,
  DeleteBucketCommand,
} from '@aws-sdk/client-s3';
import { NodeHttpHandler } from '@smithy/node-http-handler';

const S3_PORT = 10213;
const BUCKET = 'vitest-bucket';
const KEY = 'hello.txt';
const BODY = 'Hello from vitest S3 test';

const FAULT_PORTS = {
  SLOW_RESPONSE:        10205,
  RESPONSE_HEADER_ONLY: 10207,
  ACCEPT_BUT_SILENT:    10209,
};

function createS3Client(port = S3_PORT, options = {}) {
  const config = {
    region: 'ap-northeast-1',
    endpoint: `http://localhost:${port}`,
    credentials: {
      accessKeyId: 'dummy',
      secretAccessKey: 'dummy',
    },
    forcePathStyle: true,
    maxAttempts: 1,
  };
  if (options.requestTimeout) {
    config.requestHandler = new NodeHttpHandler({
      requestTimeout: options.requestTimeout,
      connectionTimeout: options.connectionTimeout ?? 5000,
      throwOnRequestTimeout: options.throwOnRequestTimeout ?? false,
    });
  }
  return new S3Client(config);
}

// --- Normal operation ---

describe('S3 Mock (port 10213)', () => {
  let s3;

  beforeAll(() => {
    s3 = createS3Client();
  });

  afterAll(async () => {
    try {
      await s3.send(new DeleteObjectCommand({ Bucket: BUCKET, Key: KEY }));
      await s3.send(new DeleteBucketCommand({ Bucket: BUCKET }));
    } catch (_) {
      // cleanup best-effort
    }
  });

  test('CreateBucket should succeed', async () => {
    const result = await s3.send(new CreateBucketCommand({
      Bucket: BUCKET,
    }));
    expect(result.$metadata.httpStatusCode).toBeLessThan(300);
  });

  test('PutObject should upload an object', async () => {
    const result = await s3.send(new PutObjectCommand({
      Bucket: BUCKET,
      Key: KEY,
      Body: BODY,
    }));
    expect(result.$metadata.httpStatusCode).toBeLessThan(300);
  });

  test('ListBuckets should include the created bucket', async () => {
    const result = await s3.send(new ListBucketsCommand({}));
    expect(result.Buckets).toBeDefined();
    const names = result.Buckets.map(b => b.Name);
    expect(names).toContain(BUCKET);
  });

  test('ListObjects should include the uploaded object', async () => {
    const result = await s3.send(new ListObjectsCommand({
      Bucket: BUCKET,
    }));
    expect(result.Contents).toBeDefined();
    const keys = result.Contents.map(o => o.Key);
    expect(keys).toContain(KEY);
  });

  test('GetObject should return the object content', async () => {
    const result = await s3.send(new GetObjectCommand({
      Bucket: BUCKET,
      Key: KEY,
    }));
    const body = await result.Body.transformToString();
    expect(body).toBe(BODY);
  });

  test('DeleteObject should succeed', async () => {
    const result = await s3.send(new DeleteObjectCommand({
      Bucket: BUCKET,
      Key: KEY,
    }));
    expect(result.$metadata.httpStatusCode).toBeLessThan(300);
  });

  test('DeleteBucket should succeed', async () => {
    const result = await s3.send(new DeleteBucketCommand({
      Bucket: BUCKET,
    }));
    expect(result.$metadata.httpStatusCode).toBeLessThan(300);
  });
});

// --- Fault injection: demonstrating the default timeout problem ---
//
// AWS SDK v3 (NodeHttpHandler) has DEFAULT_REQUEST_TIMEOUT = 0 (no timeout).
// This means requests to misbehaving servers will hang indefinitely.
//
// The tests below demonstrate:
//   1. Default config HANGS (detected by external safety net)
//   2. Properly configured requestTimeout throws promptly
//
// For S3 GetObject, there is an additional subtlety: SlowResponse and
// ResponseHeaderOnly return HTTP 200 + headers immediately, so send() succeeds.
// The hang occurs when reading the body stream — requestTimeout does NOT cover
// body consumption, so application-level timeouts are required.

describe('S3 + AcceptButSilent (port 10209)', () => {
  test('PITFALL: default client hangs — no built-in timeout', async () => {
    const s3 = createS3Client(FAULT_PORTS.ACCEPT_BUT_SILENT);
    const start = Date.now();
    await expect(
      s3.send(
        new CreateBucketCommand({ Bucket: 'hang-test' }),
        { abortSignal: AbortSignal.timeout(3000) },
      )
    ).rejects.toThrow();
    const elapsed = Date.now() - start;
    expect(elapsed).toBeGreaterThanOrEqual(2500);
  });

  test('FIX: requestTimeout + throwOnRequestTimeout: true', async () => {
    const s3 = createS3Client(FAULT_PORTS.ACCEPT_BUT_SILENT, {
      requestTimeout: 1000,
      throwOnRequestTimeout: true,
    });
    const start = Date.now();
    await expect(
      s3.send(new CreateBucketCommand({ Bucket: 'timeout-test' }))
    ).rejects.toThrow(/timeout/i);
    const elapsed = Date.now() - start;
    expect(elapsed).toBeLessThan(2500);
  });
});

describe('S3 + SlowResponse (port 10205) — body stream hang', () => {
  test('PITFALL: send() succeeds but body read hangs — requestTimeout does NOT cover this', async () => {
    // SlowResponse returns HTTP 200 immediately, then trickles body at 1 byte/sec.
    // send() succeeds because headers arrived — requestTimeout only covers the initial response.
    // Even with throwOnRequestTimeout: true, body consumption is outside the SDK's timeout scope.
    const s3 = createS3Client(FAULT_PORTS.SLOW_RESPONSE, {
      requestTimeout: 1000,
      throwOnRequestTimeout: true,
    });
    const result = await s3.send(new GetObjectCommand({
      Bucket: 'any', Key: 'any',
    }));
    // send() succeeded — requestTimeout didn't help because headers arrived in time.
    expect(result.$metadata.httpStatusCode).toBe(200);

    // FIX: Application-level timeout is required for body consumption.
    const start = Date.now();
    await expect(
      Promise.race([
        result.Body.transformToString(),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Body read timeout')), 2000)),
      ])
    ).rejects.toThrow(/timeout/i);
    const elapsed = Date.now() - start;
    expect(elapsed).toBeLessThan(3000);
  }, 10000);
});

describe('S3 + ResponseHeaderOnly (port 10207) — body stream hang', () => {
  test('PITFALL: send() succeeds but body read hangs — same as SlowResponse', async () => {
    // ResponseHeaderOnly sends HTTP 200 headers + 1 byte, then hangs forever.
    const s3 = createS3Client(FAULT_PORTS.RESPONSE_HEADER_ONLY, {
      requestTimeout: 1000,
      throwOnRequestTimeout: true,
    });
    const result = await s3.send(new GetObjectCommand({
      Bucket: 'any', Key: 'any',
    }));
    expect(result.$metadata.httpStatusCode).toBe(200);

    // FIX: Application-level timeout is required.
    const start = Date.now();
    await expect(
      Promise.race([
        result.Body.transformToString(),
        new Promise((_, reject) =>
          setTimeout(() => reject(new Error('Body read timeout')), 3000)),
      ])
    ).rejects.toThrow(/timeout/i);
    const elapsed = Date.now() - start;
    expect(elapsed).toBeLessThan(4000);
  }, 10000);
});
