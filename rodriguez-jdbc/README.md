# rodriguez-jdbc

Rodriguez supports JDBC Harness.

## Usage

Set a JDBC URL `jdbc:rodriguez://localhost:10202`.

### JDBC

```java
    HarnessConfig config = new HarnessConfig();
    config.setPorts(Map.of(10201, new MockDatabase()));
    harnessServer = new HarnessServer(config);
    harnessServer.start();
```

ResultSet data

```
% echo -n "SELECT id, name FROM emp" | sha1sum
```

