package net.unit8.rodriguez.jdbc.parser;

import net.unit8.rodriguez.jdbc.JDBCCommand;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Holds the result of parsing a SQL statement, including the command type and column names.
 */
public class ParseResult implements Serializable {
    /** The JDBC command type determined from the parsed SQL. */
    private JDBCCommand type;
    /** The list of column names extracted from the parsed SQL. */
    private List<String> columns;

    ParseResult() {
        columns = new ArrayList<>();
    }

    /**
     * Returns the JDBC command type determined from the parsed SQL.
     *
     * @return the JDBC command type
     */
    public JDBCCommand getType() {
        return type;
    }

    /**
     * Sets the JDBC command type.
     *
     * @param type the JDBC command type
     */
    public void setType(JDBCCommand type) {
        this.type = type;
    }

    /**
     * Returns the list of column names extracted from the parsed SQL.
     *
     * @return the column names
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Sets the list of column names.
     *
     * @param columns the column names
     */
    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
}
