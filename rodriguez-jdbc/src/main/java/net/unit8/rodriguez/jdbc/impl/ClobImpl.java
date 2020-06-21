package net.unit8.rodriguez.jdbc.impl;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Clob;
import java.sql.SQLException;

public class ClobImpl implements Clob {
    private final StringBuilder buffer;

    ClobImpl() {
        buffer = new StringBuilder();
    }
    @Override
    public long length() throws SQLException {
        return buffer.length();
    }

    @Override
    public String getSubString(long pos, int length) throws SQLException {
        return buffer.substring((int) pos, length);
    }

    @Override
    public Reader getCharacterStream() throws SQLException {
        return new StringReader(buffer.toString());
    }

    @Override
    public InputStream getAsciiStream() throws SQLException {
        return new ByteArrayInputStream(buffer.toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public long position(String searchstr, long start) throws SQLException {
        return buffer.indexOf(searchstr, (int) start);
    }

    @Override
    public long position(Clob searchstr, long start) throws SQLException {
        return buffer.indexOf(searchstr.getSubString(0, (int) searchstr.length()), (int) start);
    }

    @Override
    public int setString(long pos, String str) throws SQLException {
        buffer.replace((int) pos, (int)(pos + str.length()), str);
        return str.length();
    }

    @Override
    public int setString(long pos, String str, int offset, int len) throws SQLException {
        String s = str.substring(offset, offset + len);
        buffer.replace((int) pos, (int)(pos + s.length()), s);
        return len;
    }

    @Override
    public OutputStream setAsciiStream(long l) throws SQLException {
        return null;
    }

    @Override
    public Writer setCharacterStream(long l) throws SQLException {
        return null;
    }

    @Override
    public void truncate(long l) throws SQLException {

    }

    @Override
    public void free() throws SQLException {

    }

    @Override
    public Reader getCharacterStream(long l, long l1) throws SQLException {
        return null;
    }
}
