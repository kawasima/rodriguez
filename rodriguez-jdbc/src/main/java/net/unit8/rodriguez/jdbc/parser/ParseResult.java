package net.unit8.rodriguez.jdbc.parser;

import net.unit8.rodriguez.jdbc.JDBCCommand;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ParseResult implements Serializable {
    private JDBCCommand type;
    private List<String> columns;

    ParseResult() {
        columns = new ArrayList<>();
    }

    public JDBCCommand getType() {
        return type;
    }

    public void setType(JDBCCommand type) {
        this.type = type;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
}
