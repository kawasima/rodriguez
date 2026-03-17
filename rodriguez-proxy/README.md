# rodriguez-proxy

A fault-injecting reverse proxy extension for Rodriguez. Routes requests to an upstream service and selectively injects faults by forwarding matching paths to Rodriguez behavior ports.

Includes a drag-and-drop web UI dashboard and a REST API for programmatic rule management from test code.

## Configuration

The proxy is configured as a Rodriguez extension:

```json
{
  "extensions": {
    "proxy": {
      "port": 10220,
      "upstream": "http://localhost:8080",
      "connectTimeoutMs": 5000,
      "requestTimeoutMs": 30000,
      "controlUrl": "http://localhost:10200",
      "paths": ["/api/.*", "/health"]
    }
  }
}
```

| Field             | Default                      | Description                              |
| ----------------- | ---------------------------- | ---------------------------------------- |
| `port`            | 10220                        | Port the proxy listens on                |
| `upstream`        | (required)                   | Upstream service URL                     |
| `connectTimeoutMs`| 5000                         | Connection timeout in milliseconds       |
| `requestTimeoutMs`| 30000                        | Request timeout in milliseconds          |
| `controlUrl`      | `http://localhost:10200`     | Rodriguez control API URL                |
| `paths`           | (optional)                   | Path prefixes to display in the UI       |

## REST API

All API endpoints support CORS (`Access-Control-Allow-Origin: *`).

### List Rules

```
GET /_proxy/api/rules
```

Returns all active fault injection rules.

### Create Rule

```
POST /_proxy/api/rules
Content-Type: application/json
```

```json
{
  "pathPattern": "/api/users/.*",
  "faultType": "SlowResponse",
  "faultPort": 10205,
  "count": 5,
  "duration": "30s"
}
```

| Field         | Required | Description                                                       |
| ------------- | -------- | ----------------------------------------------------------------- |
| `pathPattern` | yes      | Regex to match request paths                                      |
| `faultType`   | yes      | Behavior name (e.g., `SlowResponse`, `BrokenJson`)                |
| `faultPort`   | no       | Rodriguez port. If omitted, resolved automatically from faultType |
| `count`       | no       | Number of requests to inject (default: 1)                         |
| `duration`    | no       | TTL: `"30s"`, `"5m"`, `"1h"`. Rule expires after this time       |

Response (201):

```json
{
  "id": "abc-123",
  "pathPattern": "/api/users/.*",
  "faultType": "SlowResponse",
  "faultPort": 10205,
  "remaining": 5,
  "duration": "PT30S"
}
```

### Delete All Rules

```
DELETE /_proxy/api/rules
```

Removes all active rules. Useful for test teardown. Returns 204.

### Delete Rule

```
DELETE /_proxy/api/rules/{id}
```

Returns 204.

### Increment Rule Count

```
PATCH /_proxy/api/rules/{id}/increment
```

Atomically increments the remaining count.

### List Behaviors

```
GET /_proxy/api/behaviors
```

Returns available fault behaviors discovered from the Rodriguez control API.

### List Observed Paths

```
GET /_proxy/api/paths
```

Returns paths that have been successfully proxied (HTTP 200-399).

## Server-Sent Events

```
GET /_proxy/events
```

Streams real-time rule lifecycle events.

| Event Type      | Description                                    |
| --------------- | ---------------------------------------------- |
| `rule-added`    | A new rule was created                         |
| `rule-consumed` | A rule matched a request (remaining decreased) |
| `rule-removed`  | A rule was deleted or expired                  |
| `path-observed` | A new path was successfully proxied            |

## Web Dashboard

Access the dashboard at `http://localhost:10220/_proxy/ui/`.

Features:

- Drag-and-drop fault behavior cards onto request paths
- Real-time event log via SSE
- Add custom path patterns
- View and manage active rules

## Usage from Test Code

```bash
# Inject a fault for the next 3 requests to /api/users
curl -X POST http://localhost:10220/_proxy/api/rules \
  -H 'Content-Type: application/json' \
  -d '{"pathPattern":"/api/users/.*","faultType":"SlowResponse","count":3}'

# Inject a fault with 30-second TTL
curl -X POST http://localhost:10220/_proxy/api/rules \
  -H 'Content-Type: application/json' \
  -d '{"pathPattern":"/api/.*","faultType":"BrokenJson","count":100,"duration":"30s"}'

# Clear all rules (test teardown)
curl -X DELETE http://localhost:10220/_proxy/api/rules
```
