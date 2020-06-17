package net.unit8.rodriguez.jdbc;

import net.unit8.rodriguez.jdbc.impl.ConnectionImpl;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class RodriguezDriver implements Driver {
    private static final RodriguezDriver INSTANCE = new RodriguezDriver();
    private static boolean registered;

    static {
        load();
    }

    @Override
    public Connection connect(String url, Properties properties) throws SQLException {
        return new ConnectionImpl(url, properties);
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        if (url == null) {
            throw new IllegalArgumentException("url is null");
        }
        return url.startsWith("jdbc:rodriguez:");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String s, Properties properties) throws SQLException {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return null;
    }

    public static synchronized Driver load() {
        try {
            if (!registered) {
                registered = true;
                DriverManager.registerDriver(INSTANCE);
            }
            return INSTANCE;
        } catch (SQLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static synchronized void unload() throws SQLException {
        if (registered) {
            registered = false;
            DriverManager.deregisterDriver(INSTANCE);
        }
    }
}
