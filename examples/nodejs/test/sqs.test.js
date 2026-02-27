import { describe, test, expect, beforeAll } from 'vitest';
import {
  SQSClient,
  CreateQueueCommand,
  SendMessageCommand,
  ReceiveMessageCommand,
  DeleteMessageCommand,
  DeleteQueueCommand,
  GetQueueUrlCommand,
} from '@aws-sdk/client-sqs';
import { NodeHttpHandler } from '@smithy/node-http-handler';

const SQS_PORT = 10214;

const FAULT_PORTS = {
  SLOW_RESPONSE:     10205,
  ACCEPT_BUT_SILENT: 10209,
};

function createSqsClient(port = SQS_PORT, options = {}) {
  const config = {
    region: 'ap-northeast-1',
    endpoint: `http://localhost:${port}`,
    credentials: {
      accessKeyId: 'dummy',
      secretAccessKey: 'dummy',
    },
    maxAttempts: 1,
  };
  if (options.requestTimeout) {
    config.requestHandler = new NodeHttpHandler({
      requestTimeout: options.requestTimeout,
      connectionTimeout: options.connectionTimeout ?? 5000,
      // PITFALL: Without this flag, requestTimeout only emits a warning — it does NOT throw.
      // This is a common misconfiguration that makes requestTimeout effectively useless.
      throwOnRequestTimeout: options.throwOnRequestTimeout ?? false,
    });
  }
  return new SQSClient(config);
}

// --- Normal operation ---

describe('SQS Mock (port 10214)', () => {
  let sqs;
  const queueUrl = `http://localhost:${SQS_PORT}/test-queue`;

  beforeAll(() => {
    sqs = createSqsClient();
  });

  test('CreateQueue should return a queue URL', async () => {
    const result = await sqs.send(new CreateQueueCommand({
      QueueName: 'test-queue',
    }));
    expect(result.QueueUrl).toBeDefined();
  });

  test('GetQueueUrl should return a queue URL', async () => {
    const result = await sqs.send(new GetQueueUrlCommand({
      QueueName: 'test-queue',
    }));
    expect(result.QueueUrl).toBeDefined();
  });

  test('SendMessage should return MessageId and MD5', async () => {
    const result = await sqs.send(new SendMessageCommand({
      QueueUrl: queueUrl,
      MessageBody: 'Hello from vitest',
    }));
    expect(result.MessageId).toBeDefined();
    expect(result.MD5OfMessageBody).toBeDefined();
  });

  test('ReceiveMessage should return messages', async () => {
    const result = await sqs.send(new ReceiveMessageCommand({
      QueueUrl: queueUrl,
    }));
    expect(result.Messages).toBeDefined();
    expect(result.Messages.length).toBeGreaterThan(0);
    expect(result.Messages[0].Body).toBeDefined();
    expect(result.Messages[0].MessageId).toBeDefined();
    expect(result.Messages[0].ReceiptHandle).toBeDefined();
  });

  test('DeleteMessage should succeed', async () => {
    const result = await sqs.send(new DeleteMessageCommand({
      QueueUrl: queueUrl,
      ReceiptHandle: 'dummy-receipt-handle',
    }));
    expect(result.$metadata.httpStatusCode).toBeLessThan(300);
  });

  test('DeleteQueue should succeed', async () => {
    const result = await sqs.send(new DeleteQueueCommand({
      QueueUrl: queueUrl,
    }));
    expect(result.$metadata.httpStatusCode).toBeLessThan(300);
  });
});

// --- Fault injection: demonstrating the default timeout problem ---
//
// AWS SDK v3 (NodeHttpHandler) has DEFAULT_REQUEST_TIMEOUT = 0 (no timeout).
// This means that without explicit timeout configuration, a request to a
// misbehaving server will hang indefinitely.
//
// The tests below demonstrate:
//   1. Default config HANGS (detected by AbortSignal as a safety net)
//   2. Properly configured requestTimeout throws promptly

describe('SQS + SlowResponse (port 10205)', () => {
  test('PITFALL: default client hangs — no built-in timeout', async () => {
    // DEFAULT_REQUEST_TIMEOUT = 0 means the SDK will wait forever.
    // We use AbortSignal as an external safety net to prove the SDK itself won't timeout.
    const sqs = createSqsClient(FAULT_PORTS.SLOW_RESPONSE);
    const start = Date.now();
    await expect(
      sqs.send(
        new CreateQueueCommand({ QueueName: 'hang-test' }),
        { abortSignal: AbortSignal.timeout(3000) },
      )
    ).rejects.toThrow();
    const elapsed = Date.now() - start;
    // The abort fired at ~3s, proving the SDK had no shorter internal timeout.
    expect(elapsed).toBeGreaterThanOrEqual(2500);
  });

  test('PITFALL: requestTimeout + throwOnRequestTimeout still hangs on slow body', async () => {
    // SlowResponse sends HTTP headers immediately, then trickles body at 1 byte/sec.
    // requestTimeout only covers "time to first response" — once headers arrive,
    // the timer is satisfied. The slow body read is NOT covered.
    // This is the same class of problem as S3 GetObject body stream hangs.
    const sqs = createSqsClient(FAULT_PORTS.SLOW_RESPONSE, {
      requestTimeout: 1000,
      throwOnRequestTimeout: true,
    });
    const start = Date.now();
    await expect(
      sqs.send(
        new CreateQueueCommand({ QueueName: 'slow-body-test' }),
        { abortSignal: AbortSignal.timeout(3000) },
      )
    ).rejects.toThrow();
    const elapsed = Date.now() - start;
    // Still ~3s — requestTimeout couldn't help because headers arrived promptly.
    // Only AbortSignal (or application-level timeout) can protect against slow body reads.
    expect(elapsed).toBeGreaterThanOrEqual(2500);
  });
});

describe('SQS + AcceptButSilent (port 10209)', () => {
  test('PITFALL: default client hangs — no built-in timeout', async () => {
    const sqs = createSqsClient(FAULT_PORTS.ACCEPT_BUT_SILENT);
    const start = Date.now();
    await expect(
      sqs.send(
        new CreateQueueCommand({ QueueName: 'hang-test' }),
        { abortSignal: AbortSignal.timeout(3000) },
      )
    ).rejects.toThrow();
    const elapsed = Date.now() - start;
    expect(elapsed).toBeGreaterThanOrEqual(2500);
  });

  test('FIX: requestTimeout + throwOnRequestTimeout: true', async () => {
    const sqs = createSqsClient(FAULT_PORTS.ACCEPT_BUT_SILENT, {
      requestTimeout: 1000,
      throwOnRequestTimeout: true,
    });
    const start = Date.now();
    await expect(
      sqs.send(new CreateQueueCommand({ QueueName: 'timeout-test' }))
    ).rejects.toThrow(/timeout/i);
    const elapsed = Date.now() - start;
    expect(elapsed).toBeLessThan(2500);
  });
});
