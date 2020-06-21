package net.unit8.rodriguez.jdbc.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TypeConverterTest {
    @Test
    void toDate() {
        assertThat(TypeConverter.toDate("2020/06/19"))
                .isEqualTo("2020-06-19");
    }

    @Test
    void toTime() {
        assertThat(TypeConverter.toTime("10:11:12"))
                .hasHourOfDay(10)
                .hasMinute(11)
                .hasSecond(12);
    }

}