package net.unit8.rodriguez.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import net.unit8.rodriguez.HarnessServer;
import net.unit8.rodriguez.configuration.HarnessConfig;
import net.unit8.rodriguez.jdbc.behavior.MockDatabase;
import net.unit8.rodriguez.behavior.NotAccept;
import net.unit8.rodriguez.behavior.RefuseConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.ConnectException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RodriguezConnectionTest {
    static HarnessServer harnessServer;

    @BeforeAll
    static void setup() {
        HarnessConfig config = new HarnessConfig();
        config.setPorts(Map.of(
                10201, new RefuseConnection(),
                10202, new MockDatabase(),
                10203, new NotAccept()));

        harnessServer = new HarnessServer(config);
        harnessServer.start();
    }

    @Test
    public void refuseConnection() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:rodriguez://localhost:10201");

        assertThatThrownBy(() -> {
            HikariDataSource ds = new HikariDataSource(config);
        }).isInstanceOf(HikariPool.PoolInitializationException.class)
                .hasCauseInstanceOf(SQLException.class)
                .hasRootCauseInstanceOf(ConnectException.class);
    }

    @Test
    public void notAccept() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:rodriguez://localhost:10203");
        config.setConnectionTimeout(1000);
        HikariDataSource ds = new HikariDataSource(config);

        assertThatThrownBy(() -> {
            try (Connection conn = ds.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name FROM dual")) {
                rs.next();
            }
        }).isInstanceOf(SQLException.class);
    }

    @Test
    public void mockDatabase() throws SQLException {
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

    @AfterAll
    static void shutdown() {
        harnessServer.shutdown();
    }

}