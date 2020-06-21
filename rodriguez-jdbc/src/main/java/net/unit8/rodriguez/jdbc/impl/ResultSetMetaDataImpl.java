package net.unit8.rodriguez.jdbc.impl;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class ResultSetMetaDataImpl implements ResultSetMetaData {
    private final List<String> columns;

    public ResultSetMetaDataImpl(List<String> columns) {
        this.columns = columns;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return columns.size();
    }

    @Override
    public boolean isAutoIncrement(int i) throws SQLException {
        return false;
    }

    @Override
    public boolean isCaseSensitive(int i) throws SQLException {
        return false;
    }

    @Override
    public boolean isSearchable(int i) throws SQLException {
        return false;
    }

    @Override
    public boolean isCurrency(int i) throws SQLException {
        return false;
    }

    @Override
    public int isNullable(int i) throws SQLException {
        return ResultSetMetaData.columnNullableUnknown;
    }

    @Override
    public boolean isSigned(int i) throws SQLException {
        return false;
    }

    @Override
    public int getColumnDisplaySize(int i) throws SQLException {
        return 255;
    }

    @Override
    public String getColumnLabel(int i) throws SQLException {
        return columns.get(i);
    }

    @Override
    public String getColumnName(int i) throws SQLException {
        return columns.get(i);
    }

    @Override
    public String getSchemaName(int i) throws SQLException {
        return "";
    }

    @Override
    public int getPrecision(int i) throws SQLException {
        return 0;
    }

    @Override
    public int getScale(int i) throws SQLException {
        return 0;
    }

    @Override
    public String getTableName(int i) throws SQLException {
        return "";
    }

    @Override
    public String getCatalogName(int i) throws SQLException {
        return "";
    }

    @Override
    public int getColumnType(int i) throws SQLException {
        return Types.VARCHAR;
    }

    @Override
    public String getColumnTypeName(int i) throws SQLException {
        return "VARCHAR";
    }

    @Override
    public boolean isReadOnly(int i) throws SQLException {
        return false;
    }

    @Override
    public boolean isWritable(int i) throws SQLException {
        return true;
    }

    @Override
    public boolean isDefinitelyWritable(int i) throws SQLException {
        return true;
    }

    @Override
    public String getColumnClassName(int i) throws SQLException {
        return "java.lang.String";
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (isWrapperFor(iface)) {
                return (T) this;
            }
        throw new SQLException(iface + " is not a wrapper class");
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface != null && iface.isAssignableFrom(getClass());
    }
}
