# Rodriguez Node.js Example

Fault injection test examples for Node.js applications using Rodriguez.
Uses [vitest](https://vitest.dev/) to verify how each fault pattern affects client code.

## Prerequisites

- Node.js 18+
- Rodriguez running (Docker Compose recommended)

```bash
# From the repository root
docker compose up -d
```

## Setup

```bash
cd examples/nodejs
npm install
```

## Run Tests

```bash
npm test
```

## Test Structure

### 1. ProductClient Tests (`test/product-client.test.js`)

Fault tolerance tests for a REST API client using native `fetch` + `AbortSignal.timeout()`.
Connects to each Rodriguez port and verifies the client handles faults correctly.

| Port | Behavior | Fault | Expected Error |
| --- | --- | --- | --- |
| 10201 | RefuseConnection | TCP connection refused | `CONNECTION_ERROR` |
| 10202 | NotAccept | Listen queue full, never accepts | `TIMEOUT` or `CONNECTION_ERROR` |
| 10203 | NoResponseAndSendRST | RST sent after 3s | `CONNECTION_ERROR` |
| 10204 | NeverDrain | Receive buffer never read | `TIMEOUT` or `CONNECTION_ERROR` |
| 10205 | SlowResponse | 1 byte/sec response | `TIMEOUT` or `BODY_READ_ERROR` |
| 10206 | ContentTypeMismatch | Returns HTTP 400 | `HTTP_ERROR` (status 400) |
| 10207 | ResponseHeaderOnly | Headers only, body incomplete | `TIMEOUT` or `BODY_READ_ERROR` |
| 10208 | BrokenJson | Incomplete JSON `{` | `INVALID_JSON` |
| 10209 | AcceptButSilent | No response after connection | `TIMEOUT` |
| 10211 | OversizedResponse | 10MB response | `RESPONSE_TOO_LARGE` |
| 10212 | RefuseAuthentication | 401 Unauthorized | `UNAUTHORIZED` (status 401) |

### 2. SQS Tests (`test/sqs.test.js`)

Normal operations and timeout pitfall demos using AWS SDK v3 (`@aws-sdk/client-sqs`).

**Normal operations (port 10214):** CreateQueue, GetQueueUrl, SendMessage, ReceiveMessage, DeleteMessage, DeleteQueue

**Fault injection tests:**

| Test Name | Port | Description |
| --- | --- | --- |
| PITFALL: default client hangs | 10205 (SlowResponse) | Default has no timeout; detected with 3s AbortSignal |
| PITFALL: requestTimeout still hangs on slow body | 10205 (SlowResponse) | `requestTimeout` + `throwOnRequestTimeout` still hangs on body read |
| PITFALL: default client hangs | 10209 (AcceptButSilent) | Default has no timeout |
| FIX: requestTimeout + throwOnRequestTimeout | 10209 (AcceptButSilent) | `throwOnRequestTimeout: true` times out in ~1s |

### 3. S3 Tests (`test/s3.test.js`)

Normal operations and timeout pitfall demos using AWS SDK v3 (`@aws-sdk/client-s3`).

**Normal operations (port 10213):** CreateBucket, PutObject, ListBuckets, ListObjects, GetObject, DeleteObject, DeleteBucket

**Fault injection tests:**

| Test Name | Port | Description |
| --- | --- | --- |
| PITFALL: default client hangs | 10209 (AcceptButSilent) | Default has no timeout |
| FIX: requestTimeout + throwOnRequestTimeout | 10209 (AcceptButSilent) | `throwOnRequestTimeout: true` times out in ~1s |
| PITFALL: send() succeeds but body read hangs | 10205 (SlowResponse) | `send()` succeeds, body read hangs; detected with `Promise.race` |
| PITFALL: send() succeeds but body read hangs | 10207 (ResponseHeaderOnly) | Same as above |

### 4. GCS Tests (`test/gcs.test.js`)

Normal operations and timeout pitfall demos using `@google-cloud/storage`.

**Normal operations (port 10215):** createBucket, file.save, getBuckets, getFiles, download, file.delete, bucket.delete

**Fault injection tests:**

| Test Name | Port | Description |
| --- | --- | --- |
| PITFALL: default client hangs | 10209 (AcceptButSilent) | Default has no timeout; detected with 3s `Promise.race` |
| FIX: application-level timeout | 10209 (AcceptButSilent) | `Promise.race` times out in ~1s |
| PITFALL: download hangs on slow body | 10205 (SlowResponse) | Body read hangs; detected with `Promise.race` |
| PITFALL: download hangs | 10207 (ResponseHeaderOnly) | Same as above |

**Key difference from AWS SDK:** `@google-cloud/storage` does not expose any per-request timeout option (`requestTimeout`, `AbortSignal`, etc.). The **only** way to protect against hangs is application-level `Promise.race`.

See [examples/README.md](../README.md) for detailed timeout pitfalls and cross-SDK comparison.
