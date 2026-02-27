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

/**
 * Represents a SQL statement with a unique identifier derived from its SHA-1 hash.
 *
 * <p>The statement is lazily parsed to determine its type ({@link JDBCCommand}) and column list.
 * The SHA-1 hash of the SQL text is used to locate corresponding CSV fixture files.</p>
 */
public class SQLStatement {
    private final String sql;
    private final String sqlId;

    private boolean parsed = false;
    private JDBCCommand type;
    private List<String> columns;

    /**
     * Constructs a new {@code SQLStatement} with the given SQL text.
     *
     * @param sql the SQL text
     */
    public SQLStatement(String sql) {
        this.sql = sql;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            this.sqlId = new BigInteger(1, digest.digest(sql.getBytes(StandardCharsets.UTF_8))).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Does not support SHA-1");
        }
    }

    /**
     * Returns the unique identifier (SHA-1 hex digest) of this SQL statement.
     *
     * @return the statement identifier
     */
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

    /**
     * Returns the list of column names extracted from this SQL statement.
     *
     * @return the column names
     */
    public List<String> getColumns() {
        synchronized (this) {
            if (!parsed) {
                parse();
            }
        }
        return columns;
    }

    /**
     * Returns the JDBC command type of this SQL statement (e.g., query or update).
     *
     * @return the JDBC command type
     */
    public JDBCCommand getType() {
        synchronized (this) {
            if (!parsed) {
                parse();
            }
        }
        return type;
    }

    /**
     * Creates a {@link BufferedReader} for the CSV fixture file corresponding to this statement.
     *
     * @param baseDirectory the base directory containing fixture files
     * @return a reader for the fixture CSV file
     */
    public BufferedReader createFixtureReader(File baseDirectory) {
        try {
            return new BufferedReader(new FileReader(new File(baseDirectory, sqlId + ".csv")));
        } catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }
    /**
     * Writes this statement's command type and SQL text to the given output stream.
     *
     * @param os the data output stream to write to
     * @throws IOException if an I/O error occurs
     */
    public void write(DataOutputStream os) throws IOException {
        os.writeInt(getType().ordinal());
        os.writeUTF(sql);
    }
}
