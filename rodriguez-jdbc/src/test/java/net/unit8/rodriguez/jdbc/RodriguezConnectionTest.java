package net.unit8.rodriguez.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.unit8.rodriguez.HarnessServer;
import net.unit8.rodriguez.configuration.HarnessConfig;
import net.unit8.rodriguez.jdbc.behavior.MockDatabase;
import net.unit8.rodriguez.behavior.NotAccept;
import net.unit8.rodriguez.behavior.RefuseConnection;
import net.unit8.rodriguez.jdbc.impl.ConnectionImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.ConnectException;
import java.sql.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RodriguezConnectionTest {
    static HarnessServer harnessServer;

    @BeforeAll
    static void setup() {
        HarnessConfig config = new HarnessConfig();
        MockDatabase mockDatabase = new MockDatabase();
        mockDatabase.setDataDirectory("src/test/resources/data");
        config.setPorts(Map.of(
                10201, new RefuseConnection(),
                10202, mockDatabase,
                10203, new NotAccept()));

        harnessServer = new HarnessServer(config);
        harnessServer.start();
    }

    @Test
    void refuseConnection() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:rodriguez://localhost:10201");

        assertThatThrownBy(() -> {
            HikariDataSource ds = new HikariDataSource(config);
        }).isInstanceOf(HikariPool.PoolInitializationException.class)
                .hasCauseInstanceOf(SQLException.class)
                .hasRootCauseInstanceOf(ConnectException.class);
    }

    @Test
    void notAccept() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:rodriguez://localhost:10203");
        config.setConnectionTimeout(1000);
        HikariDataSource ds = new HikariDataSource(config);

        assertThatThrownBy(() -> {
            try (Connection conn = ds.getConnection()) {
                ConnectionImpl connImpl = conn.unwrap(ConnectionImpl.class);
                connImpl.setSocketTimeout(3000);
                try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name FROM dual")) {
                    rs.next();
                }
            }
        }).isInstanceOf(SQLException.class);
    }

    @Test
    void statementExecuteQuery() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:rodriguez://localhost:10202");
        HikariDataSource ds = new HikariDataSource(config);

        try (Connection conn = ds.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, name FROM dual")) {
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("id")).isEqualTo("1");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("id")).isEqualTo("2");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("id")).isEqualTo("3");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("id")).isEqualTo("4");
            assertThat(rs.next()).isTrue();
            assertThat(rs.getString("id")).isEqualTo("5");
            assertThat(rs.next()).isFalse();
        }
    }

    @Test
    void preparedStatementExecuteQueryMultiThread() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:rodriguez://localhost:10202");
        HikariDataSource ds = new HikariDataSource(config);

        ExecutorService executor = Executors.newCachedThreadPool();
        Set<Future<?>> futures = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> {
                try (Connection conn = ds.getConnection();
                     PreparedStatement stmt = conn.prepareStatement("SELECT id, name FROM dual");
                     ResultSet rs = stmt.executeQuery()) {
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("id")).isEqualTo("1");
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("id")).isEqualTo("2");
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("id")).isEqualTo("3");
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("id")).isEqualTo("4");
                    assertThat(rs.next()).isTrue();
                    assertThat(rs.getString("id")).isEqualTo("5");
                    assertThat(rs.next()).isFalse();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        boolean isAllDone = futures.stream()
                .map(f -> {
                    try {
                        f.get(10, TimeUnit.SECONDS);
                        return f;
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        throw new RuntimeException(e);
                    }
                })
                .allMatch(Future::isDone);
        assertThat(isAllDone).isTrue();
    }

    @Test
    void statementExecuteUpdate() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:rodriguez://localhost:10202");
        HikariDataSource ds = new HikariDataSource(config);

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("insert into foo(id, name) values(?,?)")) {
            assertThat(stmt.executeUpdate()).isEqualTo(1);
        }

    }

    @AfterAll
    static void shutdown() {
        harnessServer.shutdown();
    }

}