# Rodriguez PHP Example

Fault injection test examples for PHP applications using Rodriguez.
Uses [PHPUnit](https://phpunit.de/) to verify how each fault pattern affects client code.

## Prerequisites

- PHP 8.1+ (cURL extension required)
- Composer
- Rodriguez running (Docker Compose recommended)

```bash
# From the repository root
docker compose up -d
```

## Setup

```bash
cd examples/php
composer install
```

## Run Tests

```bash
vendor/bin/phpunit --testdox
```

## Test Structure

### 1. ProductClient Tests (`tests/ProductClientTest.php`)

Fault tolerance tests for a REST API client using the Guzzle HTTP client.
Connects to each Rodriguez port and verifies the client handles faults correctly.

| Port | Behavior | Fault | Expected Error |
| --- | --- | --- | --- |
| 10201 | RefuseConnection | TCP connection refused | `CONNECTION_ERROR` |
| 10202 | NotAccept | Listen queue full, never accepts | `TIMEOUT` or `CONNECTION_ERROR` |
| 10203 | NoResponseAndSendRST | RST sent after 3s | `CONNECTION_ERROR` |
| 10204 | NeverDrain | Receive buffer never read | `TIMEOUT` or `CONNECTION_ERROR` |
| 10205 | SlowResponse | 1 byte/sec response | `TIMEOUT` or `CONNECTION_ERROR` |
| 10206 | ContentTypeMismatch | Returns HTTP 400 | `HTTP_ERROR` (status 400) |
| 10207 | ResponseHeaderOnly | Headers only, body incomplete | `TIMEOUT` or `CONNECTION_ERROR` |
| 10208 | BrokenJson | Incomplete JSON `{` | `INVALID_JSON` |
| 10209 | AcceptButSilent | No response after connection | `TIMEOUT` |
| 10211 | OversizedResponse | 10MB response | `RESPONSE_TOO_LARGE` |
| 10212 | RefuseAuthentication | 401 Unauthorized | `UNAUTHORIZED` (status 401) |

### 2. S3 Tests (`tests/S3Test.php`)

Normal operations and timeout pitfall demos using AWS SDK for PHP (`aws/aws-sdk-php`).

**Normal operations (port 10213):** CreateBucket, PutObject, ListBuckets, ListObjects, GetObject, DeleteObject, DeleteBucket

**Fault injection tests:**

| Test Name | Port | Description |
| --- | --- | --- |
| DefaultHangs | 10209 (AcceptButSilent) | Default has no timeout; detected with 3s timeout |
| FixWithTimeout | 10209 (AcceptButSilent) | Guzzle `timeout=1s` times out in ~1s |
| DefaultHangs | 10205 (SlowResponse) | Default has no timeout |
| FixWithTimeout | 10205 (SlowResponse) | Guzzle `timeout=2s` covers body reads too |
| DefaultHangs | 10207 (ResponseHeaderOnly) | Incomplete body hangs |

### 3. SQS Tests (`tests/SqsTest.php`)

Normal operations and timeout pitfall demos using AWS SDK for PHP (`aws/aws-sdk-php`).

**Normal operations (port 10214):** CreateQueue, GetQueueUrl, SendMessage, ReceiveMessage, DeleteMessage, DeleteQueue

**Fault injection tests:**

| Test Name | Port | Description |
| --- | --- | --- |
| DefaultHangs | 10209 (AcceptButSilent) | Default has no timeout |
| FixWithTimeout | 10209 (AcceptButSilent) | Guzzle `timeout=1s` times out in ~1s |
| DefaultHangs | 10205 (SlowResponse) | Default has no timeout |
| FixWithTimeout | 10205 (SlowResponse) | Guzzle `timeout=2s` covers body reads too |

See [examples/README.md](../README.md) for detailed AWS SDK timeout pitfalls and cross-language comparison.
