# Rodriguez

A test harness tool that adheres to the "Release It!" failure patterns.
It simulates various infrastructure failures (network, HTTP, JDBC, filesystem, AWS/GCP services)
on different ports from a single process.

## Failure Patterns

- [X] It can be refused.
- [X] It can sit in a listen queue until the caller times out.
- [X] The remote end can reply with a SYN/ACK and then never send any data.
- [X] The remote end can send nothing but RESET packets.
- [X] The remote end can report a full receive window and never drain the data.
- [X] The connection can be established, but the remote end never sends a byte of data.
- [ ] The connection can be established, but packets could be lost causing retransmit delays. (*)
- [ ] The connection can be established, but the remote end never acknowledges receiving a packet, causing endless retransmits. (*)
- [X] The service can accept a request, send response headers (supposing HTTP), and never send the response body.
- [X] The service can send one byte of the response every thirty seconds.
- [X] The service can send a response of HTML instead of the expected XML.
- [X] The service can send megabytes when kilobytes are expected.
- [X] The service can refuse all authentication credentials.

(*) These patterns cannot be simulated at the Java socket level because they involve TCP/IP kernel-level behavior.
Consider using [toxiproxy](https://github.com/Shopify/toxiproxy) or Linux traffic control (`tc netem`) for these scenarios.

## Modules

| Module | Description |
| --- | --- |
| rodriguez-core | Core framework: socket/HTTP behaviors, control server, configuration, metrics |
| rodriguez-jdbc | JDBC driver mock with CSV-based fixtures and configurable query delays |
| rodriguez-aws-sdk | S3 and SQS service mocks for AWS SDK testing |
| rodriguez-gcp | GCS mock for Google Cloud Storage testing |
| rodriguez-fuse | FUSE filesystem fault injection (SlowIO, DiskFull, CorruptedRead, etc.) |
| rodriguez-proxy | Fault-injecting reverse proxy with drag-and-drop web UI |
| rodriguez-build | Aggregates all modules for Docker and native image packaging |

## Multiple Port Support

> One trick I like is to have different port numbers indicate different kinds of misbehavior.

Rodriguez supports various failure patterns in a single process.
You can map a port to a failure pattern with the following configuration.

```json
{
  "ports": {
    "10201": {
      "type": "NotAccept"
    },
    "10202": {
      "type": "SlowResponse",
      "interval": 3000
    }
  }
}
```

## Get Started

Rodriguez requires JDK 21 or higher.

### Use JUnit

Start HarnessServer before you run tests.

```java
static HarnessServer server;

@BeforeAll
static void setup() {
    server = new HarnessServer();
    server.start();
}

@AfterAll
static void tearDown() {
    if (server != null) {
        server.shutdown();
    }
}
```

### Docker

```
docker pull kawasima/rodriguez
docker run -it --rm -p 10200-10220:10200-10220 kawasima/rodriguez
```

How to use a configuration file outside the container:

```
docker run -it --rm -v .:/app/conf -p 10200-10220:10200-10220 kawasima/rodriguez --config=/app/conf/rodriguez.json
```

### Docker Compose

```
docker compose up -d
```

See [compose.yml](compose.yml) for the default configuration.

### Native Build

Requires GraalVM 21 JDK.

```
mvn -Pgraalvm package
```

## REST API

### Display Configuration

```
curl http://localhost:10200/config | jq
```

### Display Metrics

Each behavior automatically records request-level counters. You can use these metrics to verify that your client retried the expected number of times, or that timeouts occurred as expected.

| Metric suffix | Description |
| --- | --- |
| `call` | Total number of connections/requests received |
| `handle-complete` | Responses sent successfully |
| `client-timeout` | Client disconnected (broken pipe) |
| `other-error` | Other I/O errors |

```
curl http://localhost:10200/metrics | jq
```

Example response:

```json
{
  "net.unit8.rodriguez.behavior.SlowResponse.client-timeout": {
    "count": 1
  },
  "net.unit8.rodriguez.behavior.SlowResponse.call": {
    "count": 1
  }
}
```

For example, if your HTTP client is configured with 3 retries and a 5-second timeout against SlowResponse (port 10205), you can assert that `net.unit8.rodriguez.behavior.SlowResponse.call` equals 3 and `net.unit8.rodriguez.behavior.SlowResponse.client-timeout` equals 3 after the test.

### Shutdown

```
curl -XPOST http://localhost:10200/shutdown
```

## Instability Behaviors

### Default Port Mapping

| Port | Behavior | Level |
| --- | --- | --- |
| 10200 | Control Server (REST API) | — |
| 10201 | RefuseConnection | Socket |
| 10202 | NotAccept | Socket |
| 10203 | NoResponseAndSendRST | Socket |
| 10204 | NeverDrain | Socket |
| 10205 | SlowResponse | HTTP |
| 10206 | ContentTypeMismatch | HTTP |
| 10207 | ResponseHeaderOnly | HTTP |
| 10208 | BrokenJson | HTTP |
| 10209 | AcceptButSilent | Socket |
| 10210 | MockDatabase | JDBC |
| 10211 | OversizedResponse | HTTP |
| 10212 | RefuseAuthentication | HTTP |
| 10213 | S3Mock | AWS |
| 10214 | SQSMock | AWS |
| 10215 | GCSMock | GCP |
| 10220 | Fault-Injecting Reverse Proxy | Extension |

### RefuseConnection

Default port: 10201

Refuse a TCP connection.

### NotAccept

Default port: 10202

TCP connection can be established but the remote end doesn't accept.

### NoResponseAndSendRST

Default port: 10203

TCP connection is established but the server socket doesn't reply and sends a RST packet.

| Property | Description | Default |
| --- | --- | --- |
| delay | Delay before sending RST in milliseconds | 3000 |

### NeverDrain

Default port: 10204

TCP connection can be established but the remote end doesn't read the packet.

### AcceptButSilent

Default port: 10209

TCP connection is established and the server reads the request, but never sends any response data.
This simulates a service that completes the TCP handshake and accepts the connection,
but remains completely silent from the client's perspective.

### SlowResponse (HTTP)

Default port: 10205

The HTTP request is accepted and the server responds successfully, but very slowly.

| Property | Description | Default |
| --- | --- | --- |
| interval | Delay between each byte in milliseconds | 1000 |

### ContentTypeMismatch (HTTP)

Default port: 10206

The HTTP request is accepted but the server returns a response with a mismatched Content-Type header
(e.g., HTML body labeled as JSON).

| Property | Description | Default |
| --- | --- | --- |
| responseStatus | The HTTP status code | 400 |
| responseBody | The response body | `<html><body>unknown error</body></html>` |
| contentType | The Content-Type header value | application/json |
| delay | Delay before responding in milliseconds | 0 |

### ResponseHeaderOnly (HTTP)

Default port: 10207

The HTTP request is accepted and the server sends response headers, but never sends the response body.

### BrokenJson (HTTP)

Default port: 10208

The HTTP request is accepted and the server sends a truncated JSON response (`{`).

### OversizedResponse (HTTP)

Default port: 10211

The HTTP request is accepted and the server sends a response with an unexpectedly large body.
This simulates a service that sends megabytes when kilobytes are expected.

| Property | Description | Default |
| --- | --- | --- |
| responseSize | The total size of the response body in bytes | 10485760 (10 MB) |
| contentType | The Content-Type header value | application/octet-stream |
| chunkSize | The size of each write chunk in bytes | 8192 |

### RefuseAuthentication (HTTP)

Default port: 10212

The HTTP request is accepted but the server always refuses authentication,
returning a 401 Unauthorized response with a WWW-Authenticate header.

| Property | Description | Default |
| --- | --- | --- |
| responseStatus | The HTTP status code | 401 |
| wwwAuthenticate | The WWW-Authenticate header value | Bearer realm="rodriguez" |
| responseBody | The response body | {"error":"unauthorized","message":"Authentication credentials were refused"} |
| contentType | The Content-Type header value | application/json |
| delay | Delay before responding in milliseconds | 0 |

### MockDatabase (JDBC)

Default port: 10210

This behavior simulates slow queries in JDBC.
Set the JDBC URL to `jdbc:rodriguez://localhost:10210` to connect to the mock database.
Rodriguez mock server returns dummy data for each query. Put the CSV files in the data directory (default: `./data`).
The naming convention of the data file is the SHA-1 hash of the query with a `.csv` extension.

| Property | Description | Default |
| --- | --- | --- |
| dataDirectory | The directory of result set files | ./data |
| delayExecution | The delayed time at executing the query | 1000 (ms) |
| delayResultSetNext | The delayed time at calling ResultSet#next | 200 (ms) |

```
cat > "data/$(echo -n 'SELECT id, name FROM emp' | sha1sum | cut -b 1-40).csv"
id,name
1,aaa
2,bbb
3,ccc
```

### S3Mock (AWS)

Default port: 10213

A mock S3 service backed by the local filesystem. Supports the following operations:

- CreateBucket / DeleteBucket / ListBuckets
- PutObject / GetObject / DeleteObject / ListObjects

| Property | Description | Default |
| --- | --- | --- |
| s3Directory | Backing directory for bucket storage | (temp directory) |
| endpointHost | Hostname for virtual-hosted-style access | localhost |

### SQSMock (AWS)

Default port: 10214

A mock SQS service with in-memory queue management. Supports both AWS Query protocol and JSON protocol.

- CreateQueue / DeleteQueue / GetQueueUrl
- SendMessage / ReceiveMessage / DeleteMessage

### GCSMock (GCP)

Default port: 10215

A mock Google Cloud Storage service backed by the local filesystem.
Uses the GCS JSON API v1 (`/storage/v1/b/...`, `/upload/storage/v1/b/...`).

- CreateBucket / DeleteBucket / ListBuckets
- UploadObject / GetObject / GetObjectMetadata / DeleteObject / ListObjects

| Property | Description | Default |
| --- | --- | --- |
| gcsDirectory | Backing directory for bucket storage | (temp directory) |

## FUSE Extension

The FUSE extension injects file I/O faults at the filesystem level using a virtual FUSE mount.

Configure it via the `extensions` section:

```json
{
  "extensions": {
    "fuse": {
      "mountPath": "/tmp/rodriguez-fuse",
      "backingPath": "/tmp/rodriguez-fuse-data",
      "faults": [
        {
          "pathPattern": ".*\\.log$",
          "operations": ["WRITE"],
          "fault": {
            "type": "DiskFull"
          }
        },
        {
          "pathPattern": ".*\\.dat$",
          "operations": ["READ"],
          "fault": {
            "type": "SlowIO",
            "delayMs": 3000
          }
        }
      ]
    }
  }
}
```

Available fault types: DiskFull, SlowIO, FileNotFound, PermissionDenied, ReadOnlyFS, IOError, TooManyOpenFiles, CorruptedRead, PartialWrite.

Available operations: READ, WRITE, OPEN, CREATE, TRUNCATE, FSYNC, FLUSH, MKDIR, UNLINK, RMDIR, RENAME, CHMOD, CHOWN.

## Reverse Proxy Extension

The reverse proxy extension injects faults into live traffic by forwarding matching requests to Rodriguez fault ports instead of the upstream service. It provides a drag-and-drop web UI for one-shot fault injection.

Configure it via the `extensions` section:

```json
{
  "extensions": {
    "proxy": {
      "port": 10220,
      "upstream": "http://localhost:8080",
      "paths": ["/api/", "/web/"],
      "connectTimeoutMs": 5000,
      "requestTimeoutMs": 30000
    }
  }
}
```

| Property | Description | Default |
| --- | --- | --- |
| port | Proxy listen port | 10220 |
| upstream | Upstream service URL to forward requests to | (required) |
| paths | Path patterns shown in the UI | [] |
| connectTimeoutMs | Upstream connection timeout in milliseconds | 5000 |
| requestTimeoutMs | Upstream request timeout in milliseconds | 30000 |

### Web UI

Open `http://localhost:10220/_proxy/ui/` to access the dashboard. Drag a fault card (e.g., SlowResponse) onto a path, specify how many requests to inject, and the proxy will forward that many matching requests to the Rodriguez fault port. The fault rule is automatically removed once consumed.

### Proxy REST API

| Method | Path | Description |
| --- | --- | --- |
| GET | `/_proxy/api/rules` | List active fault rules |
| POST | `/_proxy/api/rules` | Create a rule |
| DELETE | `/_proxy/api/rules/{id}` | Remove a rule |
| GET | `/_proxy/api/behaviors` | List available fault behaviors |

Create a rule example:

```bash
curl -X POST http://localhost:10220/_proxy/api/rules \
  -H 'Content-Type: application/json' \
  -d '{"pathPattern":"/api/.*","faultType":"SlowResponse","faultPort":10205,"count":3}'
```

The proxy also provides a real-time event stream at `/_proxy/events` (Server-Sent Events) for rule lifecycle notifications.

## Examples

The [examples](examples/) directory contains multi-language test suites that demonstrate how to test applications against Rodriguez, including cloud SDK timeout behavior across languages.

- [Node.js](examples/nodejs/) — vitest + AWS SDK v3 + `@google-cloud/storage`
- [Go](examples/go/) — go test + AWS SDK v2
- [PHP](examples/php/) — PHPUnit + AWS SDK for PHP

See [examples/README.md](examples/README.md) for a cross-language comparison of cloud SDK timeout pitfalls.
