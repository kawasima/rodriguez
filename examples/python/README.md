# Rodriguez Python Examples

Fault injection tests using `requests` and `boto3` against Rodriguez.

## Setup

```bash
pip install -r requirements.txt
```

## Run Tests

Requires Rodriguez running via Docker:

```bash
docker compose up -d   # from project root
pytest -v
```

## What These Tests Demonstrate

### HTTP Client (`requests`)

- TCP-level faults: connection refused, connection timeout, RST packet, silent accept
- HTTP-level faults: slow response, broken JSON, content type mismatch, oversized response, 401

**Key pitfall**: `requests.get(url, timeout=5)` sets *both* connect and read timeout to 5s.
Use `timeout=(connect, read)` tuple for independent control.

### AWS S3 (`boto3`)

- S3 operations against Rodriguez S3Mock (port 10213)
- Timeout configuration via `botocore.config.Config`
