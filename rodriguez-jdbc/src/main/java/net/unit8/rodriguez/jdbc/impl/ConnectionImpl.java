package net.unit8.rodriguez.jdbc.impl;

import net.unit8.rodriguez.jdbc.JDBCCommand;
import net.unit8.rodriguez.jdbc.SQLStatement;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConnectionImpl implements Connection {
    private static final Logger LOG = Logger.getLogger(ConnectionImpl.class.getName());
    private final Socket socket;
    private boolean readOnly = false;
    private boolean closed = false;
    private boolean autoCommit = false;

    private int networkTimeout = 0;
    private Executor networkTimeoutExecutor;
    private String catalog;
    private String schema;
    private int transactionIsolation = Connection.TRANSACTION_NONE;
    private Map<String, Class<?>> typeMap = new HashMap<>();
    private int holdability;

    public ConnectionImpl(String url, Properties info) throws SQLException {
        String name = url.substring("jdbc:rodriguez:".length());
        URI uri = URI.create(name);
        try {
            socket = new Socket(uri.getHost(), uri.getPort());
        } catch (IOException e) {
            throw new SQLException(e);
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        return new StatementImpl(this);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return new PreparedStatementImpl(this, new SQLStatement(sql));
    }

    @Override
    public CallableStatement prepareCall(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public String nativeSQL(String s) throws SQLException {
        assertNotClosed();
        return null;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        assertNotClosed();
        this.autoCommit = autoCommit;
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        assertNotClosed();
        return autoCommit;
    }

    @Override
    public void commit() throws SQLException {
        assertNotClosed();
    }

    @Override
    public void rollback() throws SQLException {
        assertNotClosed();
    }

    @Override
    public void close() throws SQLException {
        try (DataOutputStream is = new DataOutputStream(socket.getOutputStream())) {
            is.writeInt(JDBCCommand.CLOSE.ordinal());
            if (!socket.isClosed()) {
                socket.close();
            }
            closed = true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "connection close error", e);
            throw new SQLException(e);
        }
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return null;
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return readOnly;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        this.catalog = catalog;
    }

    @Override
    public String getCatalog() throws SQLException {
        return catalog;
    }

    @Override
    public void setTransactionIsolation(int transactionIsolation) throws SQLException {
        if (transactionIsolation < Connection.TRANSACTION_NONE || transactionIsolation >= Connection.TRANSACTION_SERIALIZABLE * 2) {
            throw new IllegalArgumentException("transaction isolation level is not valid");
        }
        this.transactionIsolation = transactionIsolation;
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return transactionIsolation;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {

    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return null;
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return typeMap;
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        typeMap = map;
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        this.holdability = holdability;
    }

    @Override
    public int getHoldability() throws SQLException {
        return holdability;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return null;
    }

    @Override
    public Savepoint setSavepoint(String s) throws SQLException {
        return null;
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {

    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {

    }

    @Override
    public Statement createStatement(int i, int i1, int i2) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql,  int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public CallableStatement prepareCall(String s, int i, int i1, int i2) throws SQLException {
        return null;
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public Clob createClob() throws SQLException {
        return null;
    }

    @Override
    public Blob createBlob() throws SQLException {
        return null;
    }

    @Override
    public NClob createNClob() throws SQLException {
        return null;
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return null;
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        return socket.isConnected();
    }

    @Override
    public void setClientInfo(String s, String s1) throws SQLClientInfoException {

    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {

    }

    @Override
    public String getClientInfo(String s) throws SQLException {
        return null;
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return null;
    }

    @Override
    public Array createArrayOf(String s, Object[] objects) throws SQLException {
        return null;
    }

    @Override
    public Struct createStruct(String s, Object[] objects) throws SQLException {
        return null;
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        this.schema = schema;
    }

    @Override
    public String getSchema() throws SQLException {
        assertNotClosed();
        return schema;
    }

    @Override
    public void abort(Executor executor) throws SQLException {

    }

    @Override
    public void setNetworkTimeout(Executor executor, int networkTimeout) throws SQLException {
        assertNotClosed();
        this.networkTimeout = networkTimeout;
        this.networkTimeoutExecutor = executor;
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        assertNotClosed();
        return networkTimeout;
    }

    Executor getNetworkTimeoutExecutor() {
        return networkTimeoutExecutor;
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

    private void assertNotClosed() throws SQLException {
        if (isClosed()) {
            throw new SQLException("Connection is closed");
        }
    }

    protected DataInputStream getInputStream() throws IOException {
        return new DataInputStream(socket.getInputStream());
    }

    protected DataOutputStream getOutputStream() throws IOException {
        return new DataOutputStream(socket.getOutputStream());
    }

    public void setSocketTimeout(int millis) throws SocketException {
        this.socket.setSoTimeout(millis);
    }
}
