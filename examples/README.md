# Rodriguez Examples

A collection of multi-language fault injection test examples using Rodriguez.
Each language demonstrates how cloud SDKs (AWS, GCP) behave under various timeout scenarios with real tests.

## Language Examples

| Directory | Language | Test Framework | SDK | Tests |
| --- | --- | --- | --- | --- |
| [nodejs/](nodejs/) | Node.js 18+ | vitest | AWS SDK v3 (`@aws-sdk/client-s3`, `@aws-sdk/client-sqs`), GCP (`@google-cloud/storage`) | 43 |
| [go/](go/) | Go 1.21+ | `go test` | AWS SDK for Go v2 (`aws-sdk-go-v2`) | 27 |
| [php/](php/) | PHP 8.1+ | PHPUnit | AWS SDK for PHP (`aws/aws-sdk-php` + Guzzle/cURL) | 33 |
| [java/](java/) | Java 21+ | Spring Boot demo | HTTP client via Rodriguez proxy | - |

## Getting Started

```bash
# Start Rodriguez from the repository root
docker compose up -d

# Run tests for each language
cd examples/nodejs && npm install && npm test
cd examples/go && go test -v -count=1 -timeout 120s ./...
cd examples/php && composer install && vendor/bin/phpunit --testdox
cd examples/java && mvn package
```

## Cloud SDK Timeout Pitfalls: Cross-SDK Comparison

We tested AWS SDKs (3 languages) and `@google-cloud/storage` against the same fault patterns (AcceptButSilent, SlowResponse, ResponseHeaderOnly) and summarized the timeout behavior differences.

### Common Pitfall: Default Timeout Is Infinite

**All three AWS SDKs default to no timeout (0 or unset).**

If the server never responds, the request hangs forever. This is not an AWS SDK bug but rather the default behavior of each language's HTTP client library.

| Language | HTTP Client | Default Timeout |
| --- | --- | --- |
| Node.js | `NodeHttpHandler` (node:http) | `DEFAULT_REQUEST_TIMEOUT = 0` |
| Go | `http.DefaultClient` | `Timeout = 0` |
| PHP | Guzzle (cURL) | `timeout = 0` |

### Node.js-Specific Pitfalls

Node.js SDK v3 has **two additional pitfalls** not found in other languages.

#### Pitfall A: Setting `requestTimeout` Does Not Throw

When `requestTimeout` is passed to `NodeHttpHandler`, it only **logs a warning** by default — the request is not aborted.

```
@smithy/node-http-handler - [WARN] a request has exceeded the configured 1000 ms
requestTimeout. Init client requestHandler with throwOnRequestTimeout=true to turn
this into an error.
```

**You must explicitly set `throwOnRequestTimeout: true` to get an error.**

```js
// Bad: timeout only produces a warning, request continues
new NodeHttpHandler({ requestTimeout: 5000 })

// Good: timeout throws an error
new NodeHttpHandler({ requestTimeout: 5000, throwOnRequestTimeout: true })
```

Go and PHP do not have this issue. Setting a timeout always produces an error.

#### Pitfall B: `requestTimeout` Does Not Cover Response Body Reads

`requestTimeout` only controls "time until response headers arrive."
If headers arrive immediately but the body is slow (or never comes), the timer is already satisfied.

- **SlowResponse**: Returns HTTP 200 + headers immediately, then sends body at 1 byte/sec
- **ResponseHeaderOnly**: Returns HTTP 200 + headers + 1 byte, then goes silent

```
SQS + SlowResponse: requestTimeout + throwOnRequestTimeout still hangs on slow body
S3  + SlowResponse: send() succeeds but body read hangs — requestTimeout does NOT cover this
```

**Workaround: Use `AbortSignal.timeout()` or `Promise.race` for application-level timeouts.**

```js
// Pass AbortSignal to send()
await client.send(command, { abortSignal: AbortSignal.timeout(5000) });

// Use Promise.race for S3 GetObject body reads
const result = await client.send(new GetObjectCommand({ Bucket, Key }));
const body = await Promise.race([
  result.Body.transformToString(),
  new Promise((_, reject) =>
    setTimeout(() => reject(new Error('Body read timeout')), 5000)),
]);
```

### Go-Specific Characteristics

Go's `http.Transport.ResponseHeaderTimeout` only covers "time until headers arrive," similar to Node.js's `requestTimeout`. However, Go provides **`http.Client.Timeout`** and **`context.WithTimeout`**, both of which cover the full request lifecycle including body reads.

```go
// Option A: Per-request timeout (recommended)
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()
result, err := client.GetObject(ctx, input)

// Option B: Client-wide timeout
httpClient := &http.Client{Timeout: 30 * time.Second}
```

### PHP-Specific Characteristics

PHP (Guzzle)'s `timeout` maps to cURL's `CURLOPT_TIMEOUT`, which covers the **entire transfer**. A single `timeout` setting handles all cases. The Node.js pitfall of "body reads not covered after headers arrive" does not exist in PHP.

```php
$client = new S3Client([
    'http' => [
        'timeout'         => 30,   // Covers entire transfer (seconds)
        'connect_timeout' => 5,    // Connection establishment timeout (seconds)
    ],
]);
```

### GCP `@google-cloud/storage` — No Timeout at Any Level

`@google-cloud/storage` uses gaxios (Axios-based) as its HTTP client. Unlike AWS SDK v3, **there is no library-level timeout configuration at all**.

- No `requestTimeout` equivalent
- No `AbortSignal` support on individual API calls
- Constructor `timeout` option has no effect
- `retryOptions` controls retry count but not per-request timeout

This makes it the most dangerous SDK tested — every operation can hang indefinitely with no built-in way to prevent it.

**The only workaround is application-level `Promise.race`:**

```js
import { Storage } from '@google-cloud/storage';

const storage = new Storage({
  projectId: 'my-project',
  retryOptions: { maxRetries: 3 },
});

// Wrap every operation with a timeout
async function withTimeout(promise, ms) {
  return Promise.race([
    promise,
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error('GCS operation timeout')), ms)),
  ]);
}

const [buckets] = await withTimeout(storage.getBuckets(), 5000);
const [contents] = await withTimeout(
  storage.bucket('b').file('key').download(), 10000);
```

### Behavior Comparison by Fault Pattern

#### No Response (AcceptButSilent) — Server accepts connection but never responds

| Setting | AWS SDK v3 (Node.js) | AWS SDK (Go) | AWS SDK (PHP) | GCP Storage (Node.js) |
| --- | --- | --- | --- | --- |
| Default | Hangs | Hangs | Hangs | Hangs |
| `requestTimeout` / `ResponseHeaderTimeout` only | Warning only | Hangs | — | N/A |
| `requestTimeout` + `throwOnRequestTimeout` | **Timeout** | — | — | N/A |
| `http.Client.Timeout` / Guzzle `timeout` | — | **Timeout** | **Timeout** | N/A |
| `AbortSignal` / `Promise.race` / `context.WithTimeout` | **Timeout** | **Timeout** | — | **Timeout** |

#### Slow Body (SlowResponse) — Headers arrive immediately, body sent at 1 byte/sec

| Setting | AWS SDK v3 (Node.js) | AWS SDK (Go) | AWS SDK (PHP) | GCP Storage (Node.js) |
| --- | --- | --- | --- | --- |
| Default | Hangs | Hangs | Hangs | Hangs |
| `requestTimeout` / `ResponseHeaderTimeout` only | Warning only | Hangs (headers arrived) | — | N/A |
| `requestTimeout` + `throwOnRequestTimeout` | **Hangs (headers arrived)** | — | — | N/A |
| `http.Client.Timeout` / Guzzle `timeout` | — | **Timeout** | **Timeout** | N/A |
| `AbortSignal` / `Promise.race` / `context.WithTimeout` | **Timeout** | **Timeout** | — | **Timeout** |

#### Incomplete Body (ResponseHeaderOnly) — Headers + 1 byte, then silence

| Setting | AWS SDK v3 (Node.js) | AWS SDK (Go) | AWS SDK (PHP) | GCP Storage (Node.js) |
| --- | --- | --- | --- | --- |
| Default | Hangs | Hangs | Hangs | Hangs |
| `requestTimeout` + `throwOnRequestTimeout` | **Hangs (headers arrived)** | — | — | N/A |
| `http.Client.Timeout` / Guzzle `timeout` | — | **Timeout** | **Timeout** | N/A |
| `AbortSignal` / `Promise.race` / `context.WithTimeout` | **Timeout** | **Timeout** | — | **Timeout** |

### Pitfall Count Comparison

| Pitfall | AWS SDK v3 (Node.js) | AWS SDK (Go) | AWS SDK (PHP) | GCP Storage (Node.js) |
| --- | --- | --- | --- | --- |
| Default timeout is infinite | Yes | Yes | Yes | **Yes** |
| Timeout setting doesn't throw | **Yes** | No | No | N/A (no setting) |
| Header timeout doesn't cover body | **Yes** | Yes | No | N/A (no setting) |
| Library-level timeout covers body | **No** | Yes | Yes | **No** |
| Any library-level timeout exists | Yes | Yes | Yes | **No** |
| **Workaround** | `AbortSignal` | `context.WithTimeout` | (built-in) | **`Promise.race` only** |

### Recommended Settings

#### Node.js (AWS SDK v3)

```js
import { NodeHttpHandler } from '@smithy/node-http-handler';

const client = new S3Client({
  requestHandler: new NodeHttpHandler({
    requestTimeout: 5000,
    connectionTimeout: 3000,
    throwOnRequestTimeout: true,  // Don't forget this
  }),
  maxAttempts: 3,
});

// Always use application-level timeout for body reads
await client.send(command, { abortSignal: AbortSignal.timeout(5000) });
```

#### Node.js (@google-cloud/storage)

```js
import { Storage } from '@google-cloud/storage';

const storage = new Storage({
  projectId: 'my-project',
  retryOptions: { maxRetries: 3 },
});

// No library-level timeout exists — wrap EVERY call with Promise.race
async function withTimeout(promise, ms) {
  return Promise.race([
    promise,
    new Promise((_, reject) =>
      setTimeout(() => reject(new Error('GCS operation timeout')), ms)),
  ]);
}

const [buckets] = await withTimeout(storage.getBuckets(), 5000);
const [contents] = await withTimeout(
  storage.bucket('b').file('key').download(), 10000);
```

#### Go

```go
// Option A: Per-request timeout (recommended)
ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
defer cancel()
result, err := client.GetObject(ctx, input)

// Option B: Client-wide timeout
httpClient := &http.Client{
    Timeout: 30 * time.Second,
    Transport: &http.Transport{
        DialContext: (&net.Dialer{Timeout: 3 * time.Second}).DialContext,
        TLSHandshakeTimeout:   3 * time.Second,
        ResponseHeaderTimeout: 10 * time.Second,
    },
}
client := s3.NewFromConfig(cfg, func(o *s3.Options) {
    o.HTTPClient = httpClient
})
```

#### PHP

```php
$client = new S3Client([
    'region' => 'ap-northeast-1',
    'http' => [
        'timeout'         => 30,
        'connect_timeout' => 5,
    ],
    'retries' => 3,
]);
```

## Rodriguez Port Mapping

| Port | Behavior |
| --- | --- |
| 10200 | Control port |
| 10201 | RefuseConnection |
| 10202 | NotAccept |
| 10203 | NoResponseAndSendRST |
| 10204 | NeverDrain |
| 10205 | SlowResponse |
| 10206 | ContentTypeMismatch |
| 10207 | ResponseHeaderOnly |
| 10208 | BrokenJson |
| 10209 | AcceptButSilent |
| 10210 | MockDatabase (JDBC) |
| 10211 | OversizedResponse |
| 10212 | RefuseAuthentication |
| 10213 | S3 Mock |
| 10214 | SQS Mock |
| 10215 | GCS Mock |
