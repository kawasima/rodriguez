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

public class TypeConverter {
    public static int toInt(String s) {
        return Integer.parseInt(s);
    }

    public static boolean toBoolean(String s) {
        return s != null && !s.equalsIgnoreCase("false") && !s.equals("0");
    }

    public static byte toByte(String s) {
        return Byte.parseByte(s);
    }

    public static short toShort(String s) {
        return Short.parseShort(s);
    }

    public static long toLong(String s) {
        return Long.parseLong(s);
    }

    public static float toFloat(String s) {
        return Float.parseFloat(s);
    }

    public static Double toDouble(String s) {
        return Double.parseDouble(s);
    }

    public static BigDecimal toBigDecimal(String s) {
        return new BigDecimal(s);
    }

    public static byte[] toBytes(String s) {
        return null; // FIXME
    }

    public static Date toDate(String s) {
        try {
            return new Date(StdDateFormat.getDateInstance().parse(s).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Time toTime(String s) {
        try {
            return new Time(StdDateFormat.getTimeInstance().parse(s).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(s);
        }
    }

    public static Timestamp toTimestamp(String s) {
        try {
            return new Timestamp(StdDateFormat.getDateTimeInstance().parse(s).getTime());
        } catch (ParseException e) {
            throw new IllegalArgumentException(s);
        }
    }

    public static InputStream toAsciiStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    public static InputStream toUnicodeStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
