# TODO: Proposed Extensions

## High Priority

### Resilience Extension (`rodriguez-resilience`)

Simulate realistic service degradation patterns for testing circuit breakers, retry logic, and backpressure handling.

**Fault patterns:**

- **RateLimited** — Return HTTP 429 Too Many Requests with `Retry-After` header
- **IntermittentFailure** — Fail probabilistically (e.g., 1 in N requests)
- **DegradedLatency** — Respond normally but with gradually increasing latency
- **CascadingTimeout** — Fast at first, slowing down as request count increases (backpressure simulation)
- **PartialSuccess** — Succeed for some items in a batch request, fail for others

### TLS Extension (`rodriguez-tls`)

Inject TLS handshake and certificate-related faults.

**Fault patterns:**

- **ExpiredCertificate** — Present an expired certificate
- **SelfSignedCertificate** — Present a self-signed certificate
- **CertificateNameMismatch** — Present a certificate that doesn't match the SNI
- **SlowHandshake** — Delay during TLS handshake
- **ProtocolDowngrade** — Only accept legacy TLS versions
- **HandshakeReset** — Send RST during handshake

## Medium Priority

### DNS Extension (`rodriguez-dns`)

Run a custom DNS server to inject DNS resolution faults.

**Fault patterns:**

- **SlowResolution** — Delay DNS responses
- **NxDomain** — Return NXDOMAIN (non-existent domain)
- **ServerFailure** — Return SERVFAIL
- **WrongAddress** — Return an incorrect IP address (for failover testing)
- **TruncatedResponse** — Truncate UDP response

### gRPC / HTTP/2 Extension (`rodriguez-grpc`)

Mock gRPC services and inject HTTP/2-specific faults.

**Fault patterns:**

- **DeadlineExceeded** — Exceed gRPC deadline
- **StreamReset** — Send RST_STREAM mid-transfer
- **SlowStream** — Delay between messages in server-streaming RPC
- **MetadataOnly** — Send HEADERS frame without DATA frames
- **GoAway** — Send HTTP/2 GOAWAY frame (force connection shutdown)
- **FlowControlExhaustion** — Exhaust HTTP/2 flow control window

## Low Priority

### Cache Protocol Extension (`rodriguez-cache`)

Mock Redis/Memcached protocols to inject cache-layer faults.

**Fault patterns:**

- **SlowResponse** — Delay GET/SET responses
- **ConnectionDrop** — Drop connection after receiving a command
- **OutOfMemory** — Return Redis `OOM command not allowed` error
- **ReadOnly** — Return `READONLY You can't write against a read only replica` error
- **ClusterMovedError** — Return `MOVED` / `ASK` errors (cluster redirection)
- **PubSubInterruption** — Drop connection during Pub/Sub subscription

### Messaging Protocol Extension (`rodriguez-messaging`)

Mock Kafka/AMQP (RabbitMQ) protocols to inject messaging faults.

**Fault patterns:**

- **SlowConsume** — Delay message delivery
- **DuplicateDelivery** — Deliver the same message multiple times (test at-least-once idempotency)
- **RebalanceStorm** — Trigger frequent Kafka consumer group rebalances
- **BrokerUnavailable** — Simulate broker unavailability
- **MessageCorruption** — Corrupt part of the message body
