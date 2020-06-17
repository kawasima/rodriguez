package net.unit8.rodriguez.jdbc.parser;

import net.unit8.rodriguez.jdbc.SQLExecutionType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ParseResult implements Serializable {
    private SQLExecutionType type;
    private List<String> columns;

    ParseResult() {
        columns = new ArrayList<>();
    }

    public SQLExecutionType getType() {
        return type;
    }

    public void setType(SQLExecutionType type) {
        this.type = type;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
}
