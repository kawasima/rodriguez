package net.unit8.rodriguez.jdbc.impl;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;

/**
 * Utility class for converting string values to various Java and SQL types.
 *
 * <p>Used by the mock JDBC result set to convert CSV fixture data to the requested Java types.</p>
 */
public class TypeConverter {
    /**
     * Constructs a new {@code TypeConverter}.
     */
    public TypeConverter() {
    }

    /**
     * Converts a string to an int.
     *
     * @param s the string to convert
     * @return the int value
     * @throws SQLException if the string cannot be parsed as an int
     */
    public static int toInt(String s) throws SQLException {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new SQLException("Cannot convert '" + s + "' to int", e);
        }
    }

    /**
     * Converts a string to a boolean.
     *
     * @param s the string to convert
     * @return {@code false} if the string is null, "false" (case-insensitive), or "0"; {@code true} otherwise
     */
    public static boolean toBoolean(String s) {
        return s != null && !s.equalsIgnoreCase("false") && !s.equals("0");
    }

    /**
     * Converts a string to a byte.
     *
     * @param s the string to convert
     * @return the byte value
     * @throws SQLException if the string cannot be parsed as a byte
     */
    public static byte toByte(String s) throws SQLException {
        try {
            return Byte.parseByte(s);
        } catch (NumberFormatException e) {
            throw new SQLException("Cannot convert '" + s + "' to byte", e);
        }
    }

    /**
     * Converts a string to a short.
     *
     * @param s the string to convert
     * @return the short value
     * @throws SQLException if the string cannot be parsed as a short
     */
    public static short toShort(String s) throws SQLException {
        try {
            return Short.parseShort(s);
        } catch (NumberFormatException e) {
            throw new SQLException("Cannot convert '" + s + "' to short", e);
        }
    }

    /**
     * Converts a string to a long.
     *
     * @param s the string to convert
     * @return the long value
     * @throws SQLException if the string cannot be parsed as a long
     */
    public static long toLong(String s) throws SQLException {
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new SQLException("Cannot convert '" + s + "' to long", e);
        }
    }

    /**
     * Converts a string to a float.
     *
     * @param s the string to convert
     * @return the float value
     * @throws SQLException if the string cannot be parsed as a float
     */
    public static float toFloat(String s) throws SQLException {
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException e) {
            throw new SQLException("Cannot convert '" + s + "' to float", e);
        }
    }

    /**
     * Converts a string to a Double.
     *
     * @param s the string to convert
     * @return the Double value
     * @throws SQLException if the string cannot be parsed as a double
     */
    public static Double toDouble(String s) throws SQLException {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new SQLException("Cannot convert '" + s + "' to double", e);
        }
    }

    /**
     * Converts a string to a BigDecimal.
     *
     * @param s the string to convert
     * @return the BigDecimal value
     * @throws SQLException if the string cannot be parsed as a BigDecimal
     */
    public static BigDecimal toBigDecimal(String s) throws SQLException {
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            throw new SQLException("Cannot convert '" + s + "' to BigDecimal", e);
        }
    }

    /**
     * Converts a string to a byte array.
     *
     * @param s the string to convert
     * @return the byte array in UTF-8 encoding
     */
    public static byte[] toBytes(String s) {
        return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    /**
     * Converts a string to a SQL Date.
     *
     * @param s the string to convert
     * @return the SQL Date value
     * @throws SQLException if the string cannot be parsed as a date
     */
    public static Date toDate(String s) throws SQLException {
        try {
            return new Date(StdDateFormat.getDateInstance().parse(s).getTime());
        } catch (ParseException e) {
            throw new SQLException("Cannot convert '" + s + "' to Date", e);
        }
    }

    /**
     * Converts a string to a SQL Time.
     *
     * @param s the string to convert
     * @return the SQL Time value
     * @throws SQLException if the string cannot be parsed as a time
     */
    public static Time toTime(String s) throws SQLException {
        try {
            return new Time(StdDateFormat.getTimeInstance().parse(s).getTime());
        } catch (ParseException e) {
            throw new SQLException("Cannot convert '" + s + "' to Time", e);
        }
    }

    /**
     * Converts a string to a SQL Timestamp.
     *
     * @param s the string to convert
     * @return the SQL Timestamp value
     * @throws SQLException if the string cannot be parsed as a timestamp
     */
    public static Timestamp toTimestamp(String s) throws SQLException {
        try {
            return new Timestamp(StdDateFormat.getDateTimeInstance().parse(s).getTime());
        } catch (ParseException e) {
            throw new SQLException("Cannot convert '" + s + "' to Timestamp", e);
        }
    }

    /**
     * Converts a string to an ASCII input stream.
     *
     * @param s the string to convert
     * @return an input stream of the string's UTF-8 bytes
     */
    public static InputStream toAsciiStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Converts a string to a Unicode input stream.
     *
     * @param s the string to convert
     * @return an input stream of the string's UTF-8 bytes
     */
    public static InputStream toUnicodeStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
