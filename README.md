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

Rodriguez requires JDK 11 or higher.

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
% docker run -it --rm -p 10200-10210:10200-10210 kawasima/rodriguez
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

## Instability Behaviors

### RefuseConnection

Default port: 10201

Refuse a TCP connection. Client should be

### NotAccept

Default port: 10202

TCP connection can be established but the remote end doesn't accept.

### NoResponseAndSendRST

Default port: 10203

TCP connection is established but the server socket doesn't reply and send a RST packet.

### NeverDrain

Default port: 10204

TCP connection can be established but the remote end doesn't read the packet.

### ResponseHeaderOnly (HTTP)

Default port: 10207

The HTTP Request can be accepted and the server response headers, but never send the response body.

### SlowResponse (HTTP)

Default port: 10205

The HTTP Request can be accepted and the server response successfully, but very slowly.

### MockDatabase (JDBC)

Default port: 10210

This behavior can simulate the slow query in JDBC.
When you set the JDBC url `jdbc:rodriguez://localhost:10210`, Connect the mock database.
Rodriguez mock server returns dummy data for each query. Put the csv files in the data directory (default: ./data).
The naming convention of the data file is SHA-1 of the query with `.csv` extension.

| property | description | default |
| --- | --- | --- |
| dataDirectory | The directory of result set files | ./data |
| delayExecution | The delayed time at executing the query  | 1000 (ms) |
| delayResultSetNext| The delayed time at calling the ResultSet#next | 200 (ms) |

The naming convention of the data file for a query is following:

```
% cat > "data/$(echo -n 'SELECT id, name FROM emp' | sha1sum | cut -b 1-40).csv"
id,name
1,aaa
2,bbb
3,ccc
```
