package net.unit8.rodriguez.jdbc.impl;

import net.unit8.rodriguez.jdbc.JDBCCommandStatus;
import net.unit8.rodriguez.jdbc.SQLStatement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StatementImpl implements Statement {
    private final ConnectionImpl connection;
    private boolean closed = false;
    private boolean closeOnCompletion = false;
    private int maxRows;
    private int fetchSize = 1;
    private int queryTimeout;
    private int fetchDirection = ResultSet.FETCH_FORWARD;
    private final int resultSetType;
    private final int resultSetConcurrency;

    StatementImpl(ConnectionImpl conn) {
        this(conn, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    }

    StatementImpl(ConnectionImpl connection, int resultSetType, int resultSetConcurrency) {
        this.connection = connection;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
    }

    private void checkCommandResponse(DataInputStream is) throws IOException, SQLException {
        JDBCCommandStatus status = JDBCCommandStatus.values()[is.readInt()];
        if (status == JDBCCommandStatus.TIMEOUT) {
            throw new SQLException("query timeout");
        }
    }
    protected ResultSet executeQueryInner(SQLStatement sqlStmt) throws IOException, SQLException {
        DataOutputStream os = connection.getOutputStream();
        sqlStmt.write(os);
        os.flush();

        DataInputStream is = connection.getInputStream();
        checkCommandResponse(is);

        int columnNum = is.readInt();
        List<String> columnNames = new ArrayList<>();
        for (int i = 0; i < columnNum; i++) {
            columnNames.add(is.readUTF());
        }
        ResultSetMetaDataImpl meta = new ResultSetMetaDataImpl(columnNames);
        return new ResultSetImpl(this, meta, is, os);
    }

    protected int executeUpdateInner(SQLStatement sqlStmt) throws IOException, SQLException {
        DataOutputStream os = connection.getOutputStream();
        sqlStmt.write(os);
        os.flush();

        DataInputStream is = connection.getInputStream();
        checkCommandResponse(is);
        return is.readInt();
    }


    @Override
    public ResultSet executeQuery(String sql) throws SQLException {
        assertNotClosed();
        SQLStatement sqlStatement = new SQLStatement(sql);
        try {
            return executeQueryInner(sqlStatement);
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public int executeUpdate(String sql) throws SQLException {
        assertNotClosed();
        SQLStatement sqlStatement = new SQLStatement(sql);
        try {
            return executeUpdateInner(sqlStatement);
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public int executeUpdate(String sql, int i) throws SQLException {
        assertNotClosed();
        return 0;
    }

    @Override
    public int executeUpdate(String sql, int[] ints) throws SQLException {
        assertNotClosed();
        return 0;
    }

    @Override
    public int executeUpdate(String sql, String[] strings) throws SQLException {
        assertNotClosed();
        return 0;
    }

    @Override
    public boolean execute(String sql, int i) throws SQLException {
        assertNotClosed();
        return false;
    }

    @Override
    public boolean execute(String sql, int[] ints) throws SQLException {
        assertNotClosed();
        return false;
    }

    @Override
    public boolean execute(String sql, String[] strings) throws SQLException {
        assertNotClosed();
        return false;
    }

    @Override
    public void close() throws SQLException {
        closed = true;
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        return 0;
    }

    @Override
    public void setMaxFieldSize(int i) throws SQLException {

    }

    @Override
    public int getMaxRows() throws SQLException {
        assertNotClosed();
        return maxRows;
    }

    @Override
    public void setMaxRows(int maxRows) throws SQLException {
        assertNotClosed();
        this.maxRows = maxRows;
    }

    @Override
    public void setEscapeProcessing(boolean b) throws SQLException {
        assertNotClosed();
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        assertNotClosed();
        return queryTimeout;
    }

    @Override
    public void setQueryTimeout(int queryTimeout) throws SQLException {
        assertNotClosed();
        this.queryTimeout = queryTimeout;
    }

    @Override
    public void cancel() throws SQLException {
        assertNotClosed();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public void setCursorName(String s) throws SQLException {
        assertNotClosed();
    }

    @Override
    public boolean execute(String sql) throws SQLException {
        assertNotClosed();
        return false;
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public int getUpdateCount() throws SQLException {
        assertNotClosed();
        return 0;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        assertNotClosed();
        return false;
    }

    @Override
    public void setFetchDirection(int fetchDirection) throws SQLException {
        assertNotClosed();
        this.fetchDirection = fetchDirection;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        assertNotClosed();
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
    public int getResultSetConcurrency() throws SQLException {
        return resultSetConcurrency;
    }

    @Override
    public int getResultSetType() throws SQLException {
        return resultSetType;
    }

    @Override
    public void addBatch(String s) throws SQLException {

    }

    @Override
    public void clearBatch() throws SQLException {

    }

    @Override
    public int[] executeBatch() throws SQLException {
        return new int[0];
    }

    @Override
    public Connection getConnection() throws SQLException {
        return null;
    }

    @Override
    public boolean getMoreResults(int i) throws SQLException {
        return false;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        return null;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        return 0;
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public void setPoolable(boolean b) throws SQLException {

    }

    @Override
    public boolean isPoolable() throws SQLException {
        return false;
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        assertNotClosed();
        closeOnCompletion = true;
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        assertNotClosed();
        return closeOnCompletion;
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

    protected void assertNotClosed() throws SQLException{
        if (isClosed()) {
            throw new SQLException("Statement closed");
        }
    }
}
