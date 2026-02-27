package net.unit8.rodriguez.jdbc.impl;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
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
     */
    public static int toInt(String s) {
        return Integer.parseInt(s);
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
     */
    public static byte toByte(String s) {
        return Byte.parseByte(s);
    }

    /**
     * Converts a string to a short.
     *
     * @param s the string to convert
     * @return the short value
     */
    public static short toShort(String s) {
        return Short.parseShort(s);
    }

    /**
     * Converts a string to a long.
     *
     * @param s the string to convert
     * @return the long value
     */
    public static long toLong(String s) {
        return Long.parseLong(s);
    }

    /**
     * Converts a string to a float.
     *
     * @param s the string to convert
     * @return the float value
     */
    public static float toFloat(String s) {
        return Float.parseFloat(s);
    }

    /**
     * Converts a string to a Double.
     *
     * @param s the string to convert
     * @return the Double value
     */
    public static Double toDouble(String s) {
        return Double.parseDouble(s);
    }

    /**
     * Converts a string to a BigDecimal.
     *
     * @param s the string to convert
     * @return the BigDecimal value
     */
    public static BigDecimal toBigDecimal(String s) {
        return new BigDecimal(s);
    }

    /**
     * Converts a string to a byte array.
     *
     * @param s the string to convert
     * @return the byte array, or {@code null} (not yet implemented)
     */
    public static byte[] toBytes(String s) {
        return null; // FIXME
    }

    /**
     * Converts a string to a SQL Date.
     *
     * @param s the string to convert
     * @return the SQL Date value
     */
    public static Date toDate(String s) {
        try {
            return new Date(StdDateFormat.getDateInstance().parse(s).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Converts a string to a SQL Time.
     *
     * @param s the string to convert
     * @return the SQL Time value
     */
    public static Time toTime(String s) {
        try {
            return new Time(StdDateFormat.getTimeInstance().parse(s).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(s);
        }
    }

    /**
     * Converts a string to a SQL Timestamp.
     *
     * @param s the string to convert
     * @return the SQL Timestamp value
     */
    public static Timestamp toTimestamp(String s) {
        try {
            return new Timestamp(StdDateFormat.getDateTimeInstance().parse(s).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(s);
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
