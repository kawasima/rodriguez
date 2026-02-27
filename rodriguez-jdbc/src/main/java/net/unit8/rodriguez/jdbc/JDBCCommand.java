package net.unit8.rodriguez.jdbc;

/**
 * Represents the types of JDBC commands exchanged between the client driver and the mock database server.
 */
public enum JDBCCommand {
    /** Executes a SQL query that returns a result set. */
    EXECUTE_QUERY,
    /** Executes a SQL update (INSERT, UPDATE, DELETE) that returns an update count. */
    EXECUTE_UPDATE,
    /** Advances the cursor to the next row in a result set. */
    RS_NEXT,
    /** Sets the query timeout for subsequent executions. */
    QUERY_TIMEOUT,
    /** Closes the connection. */
    CLOSE,
}
