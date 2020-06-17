# Rodriguez

A test harness tool that adhere to the "Release It!".

## Failure patterns

- [X] It can be refused.
- [X] It can sit in a listen queue until the caller times out.
- [ ] The remote end can reply with a SYN/ACK and then never send any data.
- [X] The remote end can send nothing but RESET packets.
- [X] The remote end can report a full receive window and never drain the data.
- [ ] The connection can be established, but the remote end never sends a byte of data.
- [ ] The connection can be established, but packets could be lost causing retransmit delays.
- [ ] The connection can be established, but the remote end never acknowledges receiving a packet, causing endless retransmits.
- [X] The service can accept a request, send response headers (supposing HTTP), and never send the response body.
- [X] The service can send one byte of the response every thirty seconds.
- [X] The service can send a response of HTML instead of the expected XML.
- [ ] The service can send megabytes when kilobytes are expected.
- [ ] The service can refuse all authentication credentials.

## Multiple port support

> One trick I like is to have different port numbers indicate different kinds of misbehavior.

Rodriguez supports various failure patterns by a single process.
You can map a port to a failure pattern as following configuration.

```json
{
  "ports": {
    "10201": {
      "type": "net.unit8.rodriguez.behavior.NotAccept"
    },
    "10202": {
      "type": "net.unit8.rodriguez.behavior.SlowResponse",
      "interval": 3000
    }
  }
}
```

## Get started

### Use JUnit

Start HarnessServer before you run tests.

```
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
% docker pull kawasima/rodriguez
% docker run -it --rm -p 10200-10208:10200-10208 kawasima/rodriguez
```
### Native build

```
% mvn -Pgraalvm package
```

## REST API

### Display configuration

```
% curl http://localhost:10200/config | jq
{
  "ports": {
    "10201": {
      "type": "net.unit8.rodriguez.behavior.RefuseConnection"
    },
    "10202": {
      "type": "net.unit8.rodriguez.behavior.NotAccept"
    },
    "10203": {
      "type": "net.unit8.rodriguez.behavior.NoResponseAndSendRST",
      "delay": 5000
    },
    "10204": {
      "type": "net.unit8.rodriguez.behavior.NeverDrain"
    },
    "10205": {
      "type": "net.unit8.rodriguez.behavior.SlowResponse",
      "interval": 3000
    },
    "10206": {
      "type": "net.unit8.rodriguez.behavior.ContentTypeMismatch",
      "responseStatus": 400,
      "responseBody": "<html><body>unknown error</body></html>",
      "contentType": "application/json",
      "delay": 1000
    },
    "10207": {
      "type": "net.unit8.rodriguez.behavior.ResponseHeaderOnly"
    },
    "10208": {
      "type": "net.unit8.rodriguez.behavior.BrokenJson"
    }
  },
  "controlPort": 10200
}
```

### Display metrics

```
% http://localhost:10200/metrics | jq
{
  "net.unit8.rodriguez.behavior.SlowResponse.client-timeout": {
    "count": 1
  },
  "net.unit8.rodriguez.behavior.SlowResponse.call": {
    "count": 1
  }
}
```

### Shutdown the rodriguez

```
curl -XPOST http://localhost:10200/shutdown
```
