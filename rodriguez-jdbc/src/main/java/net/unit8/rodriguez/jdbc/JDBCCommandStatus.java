package net.unit8.rodriguez.jdbc;

/**
 * Represents the status of a JDBC command response from the mock database server.
 */
public enum JDBCCommandStatus {
    /** The command completed successfully. */
    SUCCESS,
    /** The command timed out before completing. */
    TIMEOUT,
}
