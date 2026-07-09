package net.unit8.rodriguez.jdbc.impl;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypeConverterTest {
    @Test
    void toDate() throws SQLException {
        assertThat(TypeConverter.toDate("2020/06/19"))
                .isEqualTo("2020-06-19");
    }

    @Test
    void toTime() throws SQLException {
        assertThat(TypeConverter.toTime("10:11:12"))
                .hasHourOfDay(10)
                .hasMinute(11)
                .hasSecond(12);
    }

    @Test
    void toIntWrapsParseFailureInSQLException() {
        assertThatThrownBy(() -> TypeConverter.toInt("not-a-number"))
                .isInstanceOf(SQLException.class)
                .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void toBigDecimalWrapsParseFailureInSQLException() {
        assertThatThrownBy(() -> TypeConverter.toBigDecimal("abc"))
                .isInstanceOf(SQLException.class)
                .hasCauseInstanceOf(NumberFormatException.class);
    }

    @Test
    void toDateWrapsParseFailureInSQLException() {
        assertThatThrownBy(() -> TypeConverter.toDate("not-a-date"))
                .isInstanceOf(SQLException.class);
    }

}
