package net.unit8.rodriguez.jdbc.impl;

import net.unit8.rodriguez.jdbc.SQLStatement;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.util.Calendar;

public class PreparedStatementImpl extends StatementImpl implements PreparedStatement {
    private final SQLStatement sqlStatement;

    PreparedStatementImpl(ConnectionImpl conn, SQLStatement sqlStatement) {
        super(conn);
        this.sqlStatement = sqlStatement;
    }

    @Override
    public ResultSet executeQuery() throws SQLException {
        assertNotClosed();
        try {
            return super.executeQueryInner(sqlStatement);
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        assertNotClosed();
        throw new SQLException("Cannot call his method by PreparedStatement");
    }

    @Override
    public int executeUpdate() throws SQLException {
        assertNotClosed();
        try {
            return super.executeUpdateInner(sqlStatement);
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        assertNotClosed();
        throw new SQLException("Cannot call his method by PreparedStatement");
    }

    @Override
    public void setNull(int parameterIndex, int i1) throws SQLException {

    }

    @Override
    public void setBoolean(int i, boolean b) throws SQLException {

    }

    @Override
    public void setByte(int i, byte b) throws SQLException {

    }

    @Override
    public void setShort(int i, short i1) throws SQLException {

    }

    @Override
    public void setInt(int i, int i1) throws SQLException {

    }

    @Override
    public void setLong(int i, long l) throws SQLException {

    }

    @Override
    public void setFloat(int i, float v) throws SQLException {

    }

    @Override
    public void setDouble(int i, double v) throws SQLException {

    }

    @Override
    public void setBigDecimal(int i, BigDecimal bigDecimal) throws SQLException {

    }

    @Override
    public void setString(int i, String s) throws SQLException {

    }

    @Override
    public void setBytes(int i, byte[] bytes) throws SQLException {

    }

    @Override
    public void setDate(int i, Date date) throws SQLException {

    }

    @Override
    public void setTime(int i, Time time) throws SQLException {

    }

    @Override
    public void setTimestamp(int i, Timestamp timestamp) throws SQLException {

    }

    @Override
    public void setAsciiStream(int i, InputStream inputStream, int i1) throws SQLException {

    }

    @Override
    public void setUnicodeStream(int i, InputStream inputStream, int i1) throws SQLException {

    }

    @Override
    public void setBinaryStream(int i, InputStream inputStream, int i1) throws SQLException {

    }

    @Override
    public void clearParameters() throws SQLException {

    }

    @Override
    public void setObject(int i, Object o, int i1) throws SQLException {

    }

    @Override
    public void setObject(int i, Object o) throws SQLException {

    }

    @Override
    public boolean execute() throws SQLException {
        return false;
    }

    @Override
    public void addBatch() throws SQLException {

    }

    @Override
    public void setCharacterStream(int i, Reader reader, int i1) throws SQLException {

    }

    @Override
    public void setRef(int i, Ref ref) throws SQLException {

    }

    @Override
    public void setBlob(int i, Blob blob) throws SQLException {

    }

    @Override
    public void setClob(int i, Clob clob) throws SQLException {

    }

    @Override
    public void setArray(int i, Array array) throws SQLException {

    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        throw new SQLFeatureNotSupportedException("getParameterMetaData");
    }

    @Override
    public void setDate(int i, Date date, Calendar calendar) throws SQLException {

    }

    @Override
    public void setTime(int i, Time time, Calendar calendar) throws SQLException {

    }

    @Override
    public void setTimestamp(int i, Timestamp timestamp, Calendar calendar) throws SQLException {

    }

    @Override
    public void setNull(int i, int i1, String s) throws SQLException {

    }

    @Override
    public void setURL(int i, URL url) throws SQLException {

    }

    @Override
    public ParameterMetaData getParameterMetaData() throws SQLException {
        throw new SQLFeatureNotSupportedException("getParameterMetaData");
    }

    @Override
    public void setRowId(int i, RowId rowId) throws SQLException {

    }

    @Override
    public void setNString(int i, String s) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int i, Reader reader, long l) throws SQLException {

    }

    @Override
    public void setNClob(int i, NClob nClob) throws SQLException {

    }

    @Override
    public void setClob(int i, Reader reader, long l) throws SQLException {

    }

    @Override
    public void setBlob(int i, InputStream inputStream, long l) throws SQLException {

    }

    @Override
    public void setNClob(int i, Reader reader, long l) throws SQLException {

    }

    @Override
    public void setSQLXML(int i, SQLXML sqlxml) throws SQLException {

    }

    @Override
    public void setObject(int i, Object o, int i1, int i2) throws SQLException {

    }

    @Override
    public void setAsciiStream(int i, InputStream inputStream, long l) throws SQLException {

    }

    @Override
    public void setBinaryStream(int i, InputStream inputStream, long l) throws SQLException {

    }

    @Override
    public void setCharacterStream(int i, Reader reader, long l) throws SQLException {

    }

    @Override
    public void setAsciiStream(int i, InputStream inputStream) throws SQLException {

    }

    @Override
    public void setBinaryStream(int i, InputStream inputStream) throws SQLException {

    }

    @Override
    public void setCharacterStream(int i, Reader reader) throws SQLException {

    }

    @Override
    public void setNCharacterStream(int i, Reader reader) throws SQLException {

    }

    @Override
    public void setClob(int i, Reader reader) throws SQLException {

    }

    @Override
    public void setBlob(int i, InputStream inputStream) throws SQLException {

    }

    @Override
    public void setNClob(int i, Reader reader) throws SQLException {

    }
}
