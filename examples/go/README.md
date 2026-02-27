# Rodriguez Go Example

Fault injection test examples for Go applications using Rodriguez.
Uses `go test` to verify AWS SDK for Go v2 timeout behavior under various fault patterns.

## Prerequisites

- Go 1.21+
- Rodriguez running (Docker Compose recommended)

```bash
# From the repository root
docker compose up -d
```

## Setup

```bash
cd examples/go
go mod download
```

## Run Tests

```bash
go test -v -count=1 -timeout 120s ./...
```

## Test Structure

### 1. S3 Tests (`s3_test.go`)

Normal operations and timeout pitfall demos using AWS SDK for Go v2 (`github.com/aws/aws-sdk-go-v2/service/s3`).

**Normal operations (port 10213):** CreateBucket, PutObject, ListBuckets, ListObjects, GetObject, DeleteObject, DeleteBucket

**Fault injection tests:**

| Test Name | Port | Description |
| --- | --- | --- |
| DefaultHangs | 10209 (AcceptButSilent) | Default has no timeout; detected with 3s context |
| FixWithContextTimeout | 10209 (AcceptButSilent) | `context.WithTimeout` times out in ~1s |
| FixWithHttpClientTimeout | 10209 (AcceptButSilent) | `http.Client.Timeout` times out in ~1s |
| DefaultHangs | 10205 (SlowResponse) | GetObject succeeds, body read hangs |
| ResponseHeaderTimeoutDoesNotCoverBody | 10205 (SlowResponse) | `ResponseHeaderTimeout` is satisfied by headers; doesn't cover body reads |
| FixWithHttpClientTimeout | 10205 (SlowResponse) | `http.Client.Timeout` covers body reads |
| DefaultHangs | 10207 (ResponseHeaderOnly) | Incomplete body hangs |

### 2. SQS Tests (`sqs_test.go`)

Normal operations and timeout pitfall demos using AWS SDK for Go v2 (`github.com/aws/aws-sdk-go-v2/service/sqs`).

**Normal operations (port 10214):** CreateQueue, GetQueueUrl, SendMessage, ReceiveMessage, DeleteMessage, DeleteQueue

**Fault injection tests:**

| Test Name | Port | Description |
| --- | --- | --- |
| DefaultHangs | 10209 (AcceptButSilent) | Default has no timeout |
| FixWithContextTimeout | 10209 (AcceptButSilent) | `context.WithTimeout` times out in ~1s |
| FixWithHttpClientTimeout | 10209 (AcceptButSilent) | `http.Client.Timeout` times out in ~1s |
| DefaultHangs | 10205 (SlowResponse) | Default has no timeout |
| ResponseHeaderTimeoutDoesNotCoverBody | 10205 (SlowResponse) | `ResponseHeaderTimeout` doesn't cover body reads |
| FixWithHttpClientTimeout | 10205 (SlowResponse) | `http.Client.Timeout` covers body reads |

### 3. FUSE Tests (`main.go`)

File I/O fault injection example (requires separate FUSE configuration).

See [examples/README.md](../README.md) for detailed AWS SDK timeout pitfalls and cross-language comparison.
