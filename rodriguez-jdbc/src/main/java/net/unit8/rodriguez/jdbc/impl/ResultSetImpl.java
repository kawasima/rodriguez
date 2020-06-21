package net.unit8.rodriguez.jdbc.impl;

import net.unit8.rodriguez.jdbc.JDBCCommand;
import net.unit8.rodriguez.jdbc.JDBCCommandStatus;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class ResultSetImpl implements ResultSet {
    private final Statement stmt;
    private final ResultSetMetaData meta;
    private final DataInputStream in;
    private final DataOutputStream out;
    private final List<String> record = new ArrayList<>();
    private int fetchSize = 1;
    private int fetchDirection = ResultSet.FETCH_FORWARD;
    private final int holdability = ResultSet.HOLD_CURSORS_OVER_COMMIT;
    private boolean closed;
    private int row;

    ResultSetImpl(Statement stmt, ResultSetMetaData meta, DataInputStream in, DataOutputStream out) {
        this.stmt = stmt;
        this.meta = meta;
        this.in = in;
        this.out = out;
        row = 0;
        closed = false;
    }

    private void checkCommandResponse(DataInputStream is) throws IOException, SQLException {
        JDBCCommandStatus status = JDBCCommandStatus.values()[is.readInt()];
        if (status == JDBCCommandStatus.TIMEOUT) {
            throw new SQLException("query timeout");
        }
    }

    @Override
    public boolean next() throws SQLException {
        try {
            out.writeInt(JDBCCommand.RS_NEXT.ordinal());
            checkCommandResponse(in);
            boolean hasNext = in.readBoolean();
            if (hasNext) {
                record.clear();
                for (int i = 0; i < meta.getColumnCount(); i++) {
                    record.add(in.readUTF());
                }
                row += 1;
            }
            return hasNext;
        } catch(IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public void close() throws SQLException {
        closed = true;
    }

    @Override
    public boolean wasNull() throws SQLException {
        return false;
    }

    @Override
    public String getString(int columnIndex) throws SQLException {
        return record.get(columnIndex);
    }

    @Override
    public boolean getBoolean(int columnIndex) throws SQLException {
        return TypeConverter.toBoolean(getString(columnIndex));
    }

    @Override
    public byte getByte(int columnIndex) throws SQLException {
        return TypeConverter.toByte(getString(columnIndex));
    }

    @Override
    public short getShort(int columnIndex) throws SQLException {
        return TypeConverter.toShort(getString(columnIndex));
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
        return TypeConverter.toInt(getString(columnIndex));
    }

    @Override
    public long getLong(int columnIndex) throws SQLException {
        return TypeConverter.toLong(getString(columnIndex));
    }

    @Override
    public float getFloat(int columnIndex) throws SQLException {
        return TypeConverter.toFloat(getString(columnIndex));
    }

    @Override
    public double getDouble(int columnIndex) throws SQLException {
        return TypeConverter.toDouble(getString(columnIndex));
    }

    @Deprecated
    @Override
    public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
        return TypeConverter.toBigDecimal(getString(columnIndex));
    }

    @Override
    public byte[] getBytes(int columnIndex) throws SQLException {
        throw new SQLFeatureNotSupportedException("getBytes");
    }

    @Override
    public Date getDate(int columnIndex) throws SQLException {
        return TypeConverter.toDate(getString(columnIndex));
    }

    @Override
    public Time getTime(int columnIndex) throws SQLException {
        return TypeConverter.toTime(getString(columnIndex));
    }

    @Override
    public Timestamp getTimestamp(int columnIndex) throws SQLException {
        return TypeConverter.toTimestamp(getString(columnIndex));
    }

    @Override
    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        return null;
    }

    @Override
    public String getString(String name) throws SQLException {
        for (int i=0; i<meta.getColumnCount(); i++) {
            if (Objects.equals(meta.getColumnName(i), name)) {
                return record.get(i);
            }
        }
        throw new SQLException("Column '" + name + "' not found");
    }

    @Override
    public boolean getBoolean(String name) throws SQLException {
        return TypeConverter.toBoolean(getString(name));
    }

    @Override
    public byte getByte(String name) throws SQLException {
        return TypeConverter.toByte(getString(name));
    }

    @Override
    public short getShort(String name) throws SQLException {
        return TypeConverter.toShort(getString(name));
    }

    @Override
    public int getInt(String name) throws SQLException {
        return TypeConverter.toInt(getString(name));
    }

    @Override
    public long getLong(String name) throws SQLException {
        return TypeConverter.toLong(getString(name));
    }

    @Override
    public float getFloat(String name) throws SQLException {
        return TypeConverter.toFloat(getString(name));
    }

    @Override
    public double getDouble(String name) throws SQLException {
        return TypeConverter.toDouble(getString(name));
    }

    @Deprecated
    @Override
    public BigDecimal getBigDecimal(String name, int scale) throws SQLException {
        return TypeConverter.toBigDecimal(getString(name));
    }

    @Override
    public byte[] getBytes(String name) throws SQLException {
        throw new SQLFeatureNotSupportedException("getBytes");
    }

    @Override
    public Date getDate(String name) throws SQLException {
        return null;
    }

    @Override
    public Time getTime(String s) throws SQLException {
        return null;
    }

    @Override
    public Timestamp getTimestamp(String s) throws SQLException {
        return null;
    }

    @Override
    public InputStream getAsciiStream(String s) throws SQLException {
        return null;
    }

    @Deprecated
    @Override
    public InputStream getUnicodeStream(String s) throws SQLException {
        return null;
    }

    @Deprecated
    @Override
    public InputStream getBinaryStream(String s) throws SQLException {
        return null;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        assertNotClosed();
    }

    @Override
    public String getCursorName() throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        assertNotClosed();
        return meta;
    }

    @Override
    public Object getObject(int i) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Object getObject(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public int findColumn(String s) throws SQLException {
        assertNotClosed();
        return 0;
    }

    @Override
    public Reader getCharacterStream(int i) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Reader getCharacterStream(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
        assertNotClosed();
        return TypeConverter.toBigDecimal(getString(columnIndex));
    }

    @Override
    public BigDecimal getBigDecimal(String name) throws SQLException {
        assertNotClosed();
        return TypeConverter.toBigDecimal(getString(name));
    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        assertNotClosed();
        return row == 0;
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        throw new SQLFeatureNotSupportedException("isAfterLast");
    }

    @Override
    public boolean isFirst() throws SQLException {
        assertNotClosed();
        return row == 1;
    }

    @Override
    public boolean isLast() throws SQLException {
        throw new SQLFeatureNotSupportedException("isLast");
    }

    @Override
    public void beforeFirst() throws SQLException {
        throw new SQLFeatureNotSupportedException("beforeFirst");
    }

    @Override
    public void afterLast() throws SQLException {
        throw new SQLFeatureNotSupportedException("afterLast");

    }

    @Override
    public boolean first() throws SQLException {
        throw new SQLFeatureNotSupportedException("first");
    }

    @Override
    public boolean last() throws SQLException {
        return false;
    }

    @Override
    public int getRow() throws SQLException {
        return 0;
    }

    @Override
    public boolean absolute(int i) throws SQLException {
        return false;
    }

    @Override
    public boolean relative(int i) throws SQLException {
        return false;
    }

    @Override
    public boolean previous() throws SQLException {
        throw new SQLFeatureNotSupportedException("previous");
    }

    @Override
    public void setFetchDirection(int fetchDirection) throws SQLException {
        this.fetchDirection = fetchDirection;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        return fetchDirection;
    }

    @Override
    public void setFetchSize(int fetchSize) throws SQLException {
        this.fetchSize = fetchSize;
    }

    @Override
    public int getFetchSize() throws SQLException {
        return fetchSize;
    }

    @Override
    public int getType() throws SQLException {
        assertNotClosed();
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    @Override
    public int getConcurrency() throws SQLException {
        assertNotClosed();
        return ResultSet.CONCUR_READ_ONLY;
    }

    @Override
    public boolean rowUpdated() throws SQLException {
        assertNotClosed();
        return false;
    }

    @Override
    public boolean rowInserted() throws SQLException {
        assertNotClosed();
        return false;
    }

    @Override
    public boolean rowDeleted() throws SQLException {
        assertNotClosed();
        return false;
    }

    @Override
    public void updateNull(int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNull");
    }

    @Override
    public void updateBoolean(int i, boolean b) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBoolean");
    }

    @Override
    public void updateByte(int i, byte b) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateByte");
    }

    @Override
    public void updateShort(int i, short i1) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateShort");
    }

    @Override
    public void updateInt(int i, int i1) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateInt");
    }

    @Override
    public void updateLong(int i, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateLong");
    }

    @Override
    public void updateFloat(int i, float v) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateFloat");
    }

    @Override
    public void updateDouble(int i, double v) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateDouble");
    }

    @Override
    public void updateBigDecimal(int i, BigDecimal bigDecimal) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBigDecimal");
    }

    @Override
    public void updateString(int i, String s) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateString");
    }

    @Override
    public void updateBytes(int i, byte[] bytes) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBytes");
    }

    @Override
    public void updateDate(int i, Date date) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateDate");
    }

    @Override
    public void updateTime(int i, Time time) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateTime");
    }

    @Override
    public void updateTimestamp(int i, Timestamp timestamp) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateTimestamp");
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream, int i1) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateAsciiStream");
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream, int i1) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBinaryStream");
    }

    @Override
    public void updateCharacterStream(int i, Reader reader, int i1) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateCharacterStream");
    }

    @Override
    public void updateObject(int i, Object o, int i1) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject");
    }

    @Override
    public void updateObject(int i, Object o) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject");

    }

    @Override
    public void updateNull(String s) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNull");
    }

    @Override
    public void updateBoolean(String s, boolean b) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBoolean");
    }

    @Override
    public void updateByte(String s, byte b) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateByte");
    }

    @Override
    public void updateShort(String s, short i) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateShort");
    }

    @Override
    public void updateInt(String s, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateInt");
    }

    @Override
    public void updateLong(String s, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateLong");
    }

    @Override
    public void updateFloat(String s, float v) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateFloat");
    }

    @Override
    public void updateDouble(String s, double v) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateDouble");
    }

    @Override
    public void updateBigDecimal(String s, BigDecimal bigDecimal) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBigDecimal");
    }

    @Override
    public void updateString(String s, String s1) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateString");
    }

    @Override
    public void updateBytes(String s, byte[] bytes) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBytes");
    }

    @Override
    public void updateDate(String s, Date date) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateDate");
    }

    @Override
    public void updateTime(String s, Time time) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateTime");
    }

    @Override
    public void updateTimestamp(String s, Timestamp timestamp) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateTimestamp");
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateAsciiStream");
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBinaryStream");
    }

    @Override
    public void updateCharacterStream(String s, Reader reader, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateCharacterStream");
    }

    @Override
    public void updateObject(String s, Object o, int i) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject");
    }

    @Override
    public void updateObject(String s, Object o) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateObject");
    }

    @Override
    public void insertRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("insertRow");
    }

    @Override
    public void updateRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("updateRow");
    }

    @Override
    public void deleteRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("deleteRow");
    }

    @Override
    public void refreshRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("refreshRow");
    }

    @Override
    public void cancelRowUpdates() throws SQLException {
        throw new SQLFeatureNotSupportedException("cancelRowUpdates");
    }

    @Override
    public void moveToInsertRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("moveToInsertRow");
    }

    @Override
    public void moveToCurrentRow() throws SQLException {
        throw new SQLFeatureNotSupportedException("moveToCurrentRow");
    }

    @Override
    public Statement getStatement() throws SQLException {
        return stmt;
    }

    @Override
    public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Ref getRef(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Blob getBlob(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Clob getClob(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Array getArray(int columnIndex) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Object getObject(String s, Map<String, Class<?>> map) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Ref getRef(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Blob getBlob(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Clob getClob(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Array getArray(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Date getDate(int i, Calendar calendar) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Date getDate(String s, Calendar calendar) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Time getTime(int i, Calendar calendar) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Time getTime(String s, Calendar calendar) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Timestamp getTimestamp(int i, Calendar calendar) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Timestamp getTimestamp(String s, Calendar calendar) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public URL getURL(int i) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public URL getURL(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public void updateRef(int i, Ref ref) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateRef");
    }

    @Override
    public void updateRef(String s, Ref ref) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateRef");
    }

    @Override
    public void updateBlob(int i, Blob blob) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBlob");
    }

    @Override
    public void updateBlob(String s, Blob blob) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBlob");
    }

    @Override
    public void updateClob(int i, Clob clob) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateClob");
    }

    @Override
    public void updateClob(String s, Clob clob) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateClob");
    }

    @Override
    public void updateArray(int i, Array array) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateArray");
    }

    @Override
    public void updateArray(String s, Array array) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateArray");
    }

    @Override
    public RowId getRowId(int i) throws SQLException {
        return null;
    }

    @Override
    public RowId getRowId(String s) throws SQLException {
        return null;
    }

    @Override
    public void updateRowId(int i, RowId rowId) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateRowId");
    }

    @Override
    public void updateRowId(String s, RowId rowId) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateRowId");
    }

    @Override
    public int getHoldability() throws SQLException {
        return holdability;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public void updateNString(int i, String s) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNString");
    }

    @Override
    public void updateNString(String s, String s1) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNString");
    }

    @Override
    public void updateNClob(int i, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNClob");
    }

    @Override
    public void updateNClob(String s, NClob nClob) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNClob");
    }

    @Override
    public NClob getNClob(int i) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public NClob getNClob(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public SQLXML getSQLXML(int i) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public SQLXML getSQLXML(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public void updateSQLXML(int i, SQLXML sqlxml) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateSQLXML");
    }

    @Override
    public void updateSQLXML(String s, SQLXML sqlxml) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateSQLXML");
    }

    @Override
    public String getNString(int i) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public String getNString(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Reader getNCharacterStream(int i) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public Reader getNCharacterStream(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public void updateNCharacterStream(int i, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNCharacterStream");
    }

    @Override
    public void updateNCharacterStream(String s, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNCharacterStream");
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateAsciiStream");
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBinaryStream");
    }

    @Override
    public void updateCharacterStream(int i, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateCharacterStream");
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateAsciiStream");
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBinaryStream");
    }

    @Override
    public void updateCharacterStream(String s, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateCharacterStream");
    }

    @Override
    public void updateBlob(int i, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBlob");
    }

    @Override
    public void updateBlob(String s, InputStream inputStream, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBlob");
    }

    @Override
    public void updateClob(int i, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateClob");
    }

    @Override
    public void updateClob(String s, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateClob");
    }

    @Override
    public void updateNClob(int i, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNClob");
    }

    @Override
    public void updateNClob(String s, Reader reader, long l) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNClob");
    }

    @Override
    public void updateNCharacterStream(int i, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNCharacterStream");
    }

    @Override
    public void updateNCharacterStream(String s, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNCharacterStream");
    }

    @Override
    public void updateAsciiStream(int i, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateAsciiStream");
    }

    @Override
    public void updateBinaryStream(int i, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBinaryStream");
    }

    @Override
    public void updateCharacterStream(int i, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateCharacterStream");
    }

    @Override
    public void updateAsciiStream(String s, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateAsciiStream");
    }

    @Override
    public void updateBinaryStream(String s, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBinaryStream");
    }

    @Override
    public void updateCharacterStream(String s, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateCharacterStream");
    }

    @Override
    public void updateBlob(int i, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBlob");
    }

    @Override
    public void updateBlob(String s, InputStream inputStream) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateBlob");
    }

    @Override
    public void updateClob(int i, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateClob");
    }

    @Override
    public void updateClob(String s, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateClob");
    }

    @Override
    public void updateNClob(int i, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNClob");
    }

    @Override
    public void updateNClob(String s, Reader reader) throws SQLException {
        throw new SQLFeatureNotSupportedException("updateNClob");
    }

    @Override
    public <T> T getObject(int i, Class<T> aClass) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public <T> T getObject(String s, Class<T> aClass) throws SQLException {
        assertNotClosed();
        return null;
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

    private void assertNotClosed() throws SQLException{
        if (closed) {
            throw new SQLException("ResultSet is closed");
        }
    }
}
