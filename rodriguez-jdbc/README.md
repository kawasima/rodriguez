# rodriguez-jdbc

JDBC driver mock that returns CSV fixture data with configurable execution and iteration delays. Useful for testing slow query handling, connection pool exhaustion, and timeout behavior.

## Default Port

| Port  | Behavior     |
| ----- | ------------ |
| 10210 | MockDatabase |

## JDBC URL

```
jdbc:rodriguez://localhost:10210
```

The driver class `net.unit8.rodriguez.jdbc.RodriguezDriver` is auto-registered via `ServiceLoader`.

## Configuration

```json
{
  "ports": {
    "10210": {
      "type": "MockDatabase",
      "dataDirectory": "data",
      "delayExecution": 1000,
      "delayResultSetNext": 200
    }
  }
}
```

| Field               | Default | Description                                       |
| ------------------- | ------- | ------------------------------------------------- |
| `dataDirectory`     | `data`  | Path to directory containing CSV fixture files    |
| `delayExecution`    | 1000    | Delay in ms before returning query results        |
| `delayResultSetNext`| 200     | Delay in ms for each `ResultSet.next()` call      |

## Fixture Files

Query results are loaded from CSV files. The file name is the **SHA-1 hash** of the SQL statement.

### Creating a Fixture

1. Compute the SHA-1 of your SQL:

```bash
echo -n "SELECT id, name FROM emp" | sha1sum
# Output: 500b40b481688654ffa6b607ffdf8df4e2fe420f
```

2. Create a CSV file named `{hash}.csv` in the data directory:

```csv
id,name
1,Alice
2,Bob
3,Charlie
```

The first row is the header (column names). Subsequent rows are the data.

### File Location

Place fixture files in the `dataDirectory` path. The default is `data/` relative to the working directory.

```
data/
  500b40b481688654ffa6b607ffdf8df4e2fe420f.csv
  a1b2c3d4e5f6....csv
```

## Java Usage

```java
// Start the harness
HarnessConfig config = new HarnessConfig();
MockDatabase mockDb = new MockDatabase();
mockDb.setDataDirectory("src/test/resources/data");
mockDb.setDelayExecution(500);
config.setPorts(Map.of(10210, mockDb));
HarnessServer server = new HarnessServer(config);
server.start();

// Use standard JDBC
try (Connection conn = DriverManager.getConnection("jdbc:rodriguez://localhost:10210");
     PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM emp");
     ResultSet rs = ps.executeQuery()) {
    while (rs.next()) {
        System.out.println(rs.getInt("id") + ": " + rs.getString("name"));
    }
}
```

### With HikariCP

```java
HikariConfig hikariConfig = new HikariConfig();
hikariConfig.setJdbcUrl("jdbc:rodriguez://localhost:10210");
hikariConfig.setMaximumPoolSize(5);
hikariConfig.setConnectionTimeout(3000);

try (HikariDataSource ds = new HikariDataSource(hikariConfig);
     Connection conn = ds.getConnection()) {
    // ...
}
```

## Testing Scenarios

- **Slow query**: Set `delayExecution` to a value exceeding your application's query timeout
- **Slow iteration**: Set `delayResultSetNext` to simulate large result set reads
- **Connection pool exhaustion**: Set high delays and open many connections to exhaust pool limits
