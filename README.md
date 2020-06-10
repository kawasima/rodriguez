# Rodriguez

A test harness tool that adhere to the "Release It!".

## Failure patterns

- [ ] It can be refused.
- [ ] It can sit in a listen queue until the caller times out.
- [ ] The remote end can reply with a SYN/ACK and then never send any data.
- [ ] The remote end can send nothing but RESET packets.
- [ ] The remote end can report a full receive window and never drain the data.
- [ ] The connection can be established, but the remote end never sends a byte of data.
- [ ] The connection can be established, but packets could be lost causing retransmit delays.
- [ ] The connection can be established, but the remote end never acknowledges receiving a packet, causing endless retransmits.
- [ ] The service can accept a request, send response headers (supposing HTTP), and never send the response body.
- [ ] The service can send one byte of the response every thirty seconds.
- [ ] The service can send a response of HTML instead of the expected XML.
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
      "type": "net.unit8.rodriguez.strategy.NotAccept"
    },
    "10202": {
      "type": "net.unit8.rodriguez.strategy.SlowResponse",
      "interval": 3000
    }
  }
}
```
