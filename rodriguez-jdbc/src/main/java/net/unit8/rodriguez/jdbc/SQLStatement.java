package net.unit8.rodriguez.jdbc;

import net.sf.jsqlparser.JSQLParserException;
import net.unit8.rodriguez.jdbc.parser.ParseResult;
import net.unit8.rodriguez.jdbc.parser.SqlParser;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class SQLStatement {
    private final String sql;
    private final String sqlId;

    private boolean parsed = false;
    private JDBCCommand type;
    private List<String> columns;

    public SQLStatement(String sql) {
        this.sql = sql;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            this.sqlId = new BigInteger(1, digest.digest(sql.getBytes(StandardCharsets.UTF_8))).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Does not support SHA-1");
        }
    }

    public String getId() {
        return sqlId;
    }

    private void parse() {
        try {
            ParseResult result = new SqlParser().parse(sql);
            columns = result.getColumns();
            type = result.getType();
            parsed = true;
        } catch (JSQLParserException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public List<String> getColumns() {
        synchronized (this) {
            if (!parsed) {
                parse();
            }
        }
        return columns;
    }

    public JDBCCommand getType() {
        synchronized (this) {
            if (!parsed) {
                parse();
            }
        }
        return type;
    }

    public BufferedReader createFixtureReader(File baseDirectory) {
        try {
            return new BufferedReader(new FileReader(new File(baseDirectory, sqlId + ".csv")));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }
    public void write(DataOutputStream os) throws IOException {
        os.writeInt(getType().ordinal());
        os.writeUTF(sql);
    }
}
