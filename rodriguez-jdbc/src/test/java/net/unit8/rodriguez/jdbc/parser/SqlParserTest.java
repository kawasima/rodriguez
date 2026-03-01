package net.unit8.rodriguez.jdbc.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.unit8.rodriguez.jdbc.JDBCCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlParserTest {
    SqlParser parser;

    @BeforeEach
    void setUp() {
        parser = new SqlParser();
    }

    // ---- SELECT ----

    @Test
    void simpleSelect() throws JSQLParserException {
        ParseResult result = parser.parse("SELECT id, name FROM users");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_QUERY);
        assertThat(result.getColumns()).containsExactly("id", "name");
    }

    @Test
    void selectWithAlias() throws JSQLParserException {
        ParseResult result = parser.parse("SELECT u.id AS user_id, u.name AS user_name FROM users u");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_QUERY);
        assertThat(result.getColumns()).containsExactly("user_id", "user_name");
    }

    @Test
    void selectStar() throws JSQLParserException {
        ParseResult result = parser.parse("SELECT * FROM users");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_QUERY);
        assertThat(result.getColumns()).containsExactly("*");
    }

    @Test
    void selectWithWhere() throws JSQLParserException {
        ParseResult result = parser.parse("SELECT id FROM users WHERE id = 1");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_QUERY);
        assertThat(result.getColumns()).containsExactly("id");
    }

    @Test
    void selectWithBindParameter() throws JSQLParserException {
        ParseResult result = parser.parse("SELECT id, name FROM users WHERE id = ?");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_QUERY);
        assertThat(result.getColumns()).containsExactly("id", "name");
    }

    // ---- INSERT ----

    @Test
    void insertStatement() throws JSQLParserException {
        ParseResult result = parser.parse("INSERT INTO users (id, name) VALUES (1, 'Alice')");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_UPDATE);
        assertThat(result.getColumns()).isEmpty();
    }

    @Test
    void insertWithBindParameters() throws JSQLParserException {
        ParseResult result = parser.parse("INSERT INTO users (id, name) VALUES (?, ?)");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_UPDATE);
    }

    // ---- UPDATE ----

    @Test
    void updateStatement() throws JSQLParserException {
        ParseResult result = parser.parse("UPDATE users SET name = 'Bob' WHERE id = 1");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_UPDATE);
    }

    @Test
    void updateWithBindParameters() throws JSQLParserException {
        ParseResult result = parser.parse("UPDATE users SET name = ? WHERE id = ?");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_UPDATE);
    }

    // ---- DELETE ----

    @Test
    void deleteStatement() throws JSQLParserException {
        ParseResult result = parser.parse("DELETE FROM users WHERE id = 1");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_UPDATE);
    }

    // ---- UPSERT ----

    @Test
    void upsertStatement() throws JSQLParserException {
        ParseResult result = parser.parse(
                "INSERT INTO users (id, name) VALUES (1, 'Alice') ON DUPLICATE KEY UPDATE name = VALUES(name)");
        assertThat(result.getType()).isEqualTo(JDBCCommand.EXECUTE_UPDATE);
    }

    // ---- Error cases ----

    @Test
    void invalidSqlThrowsParserException() {
        assertThatThrownBy(() -> parser.parse("NOT VALID SQL !!!"))
                .isInstanceOf(JSQLParserException.class);
    }
}
