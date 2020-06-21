package net.unit8.rodriguez.jdbc.behavior;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import net.unit8.rodriguez.MetricsAvailable;
import net.unit8.rodriguez.SocketInstabilityBehavior;
import net.unit8.rodriguez.jdbc.JDBCCommand;
import net.unit8.rodriguez.jdbc.JDBCCommandStatus;
import net.unit8.rodriguez.jdbc.SQLStatement;
import net.unit8.rodriguez.jdbc.impl.DelayTimer;
import net.unit8.rodriguez.metrics.MetricRegistry;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MockDatabase implements SocketInstabilityBehavior, MetricsAvailable {
    private static final Logger LOG = Logger.getLogger(MockDatabase.class.getName());

    public long delayExecution = 1000L;
    public long delayResultSetNext = 200L;
    public String dataDirectory = "data";

    private final CsvMapper mapper = CsvMapper.builder().build();
    private final CsvSchema schema = CsvSchema.builder()
            .setUseHeader(true)
            .build();

    private static class StatementSession {
        private MappingIterator<Map<String, Object>> iterator;
        private BufferedReader reader;
        private List<String> columns;
    }

    private StatementSession doExecute(DataInputStream in, DataOutputStream out) throws IOException{
        SQLStatement sqlStmt = new SQLStatement(in.readUTF());
        StatementSession session = new StatementSession();
        session.reader = sqlStmt.createFixtureReader(new File(dataDirectory));
        session.iterator = mapper
                .readerWithSchemaFor(Map.class)
                .with(schema)
                .readValues(session.reader);
        CsvSchema parserSchema = (CsvSchema) session.iterator.getParserSchema();
        session.columns = StreamSupport.stream(parserSchema.spliterator(), false)
                .map(CsvSchema.Column::getName)
                .collect(Collectors.toUnmodifiableList());
        out.writeInt(session.columns.size());
        session.columns.forEach(col -> {
            try {
                out.writeUTF(col);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        session.columns = sqlStmt.getColumns();

        return session;
    }

    private void doNext(StatementSession session, DataInputStream in, DataOutputStream out) throws IOException {
        boolean hasNext = session.iterator.hasNext();
        out.writeBoolean(hasNext);
        if (!hasNext) {
            return;
        }
        Map<String, Object> record = session.iterator.next();

        for (String col : session.columns) {
            out.writeUTF(Objects.toString(record.get(col)));
        }
    }

    @Override
    public void handle(Socket socket) throws InterruptedException {
        try (DataInputStream is = new DataInputStream(socket.getInputStream());
             DataOutputStream os = new DataOutputStream(socket.getOutputStream())) {
            int queryTimeout = 0;
            DelayTimer timer = null;
            StatementSession session = null;
            while(!Thread.interrupted()) {
                if (socket.isClosed()) {
                    throw new EOFException("socket closed");
                }
                int commandIndex = is.readInt();
                if (commandIndex < 0 || commandIndex >= JDBCCommand.values().length) {
                    throw new IOException("Command is invalid [" + commandIndex + "]");
                }
                JDBCCommand command = JDBCCommand.values()[commandIndex];

                switch (command) {
                    case CLOSE: {
                        socket.close();
                        Thread.currentThread().interrupt();
                        break;
                    }
                    case EXECUTE_QUERY: {
                        if (session != null && session.reader != null) {
                            try {
                                session.reader.close();
                            } catch (IOException ignore) {
                            }
                        }
                        getMetricRegistry().counter(MetricRegistry.name(getClass(), "execute-query")).inc();
                        timer = new DelayTimer(queryTimeout);
                        if (timer.isTimeout(delayExecution)) {
                            os.writeInt(JDBCCommandStatus.TIMEOUT.ordinal());
                        } else {
                            os.writeInt(JDBCCommandStatus.SUCCESS.ordinal());
                            session = doExecute(is, os);
                        }
                        break;
                    }
                    case RS_NEXT: {
                        if (timer.isTimeout(delayResultSetNext)) {
                            os.writeInt(JDBCCommandStatus.TIMEOUT.ordinal());
                        } else {
                            os.writeInt(JDBCCommandStatus.SUCCESS.ordinal());
                            doNext(Objects.requireNonNull(session), is, os);
                        }
                        break;
                    }
                    case EXECUTE_UPDATE: {
                        if (session != null && session.reader != null) {
                            try {
                                session.reader.close();
                            } catch (IOException ignore) {
                            }
                        }
                        getMetricRegistry().counter(MetricRegistry.name(getClass(), "execute-update")).inc();
                        timer = new DelayTimer(queryTimeout);
                        String sql = is.readUTF();
                        if (timer.isTimeout(delayExecution)) {
                            os.writeInt(JDBCCommandStatus.TIMEOUT.ordinal());
                        } else {
                            os.writeInt(JDBCCommandStatus.SUCCESS.ordinal());
                            os.writeInt(1);
                        }
                        break;
                    }
                    case QUERY_TIMEOUT: {
                        queryTimeout = is.readInt();
                        break;
                    }
                    default:
                        throw new IllegalArgumentException("Unknown command: " + command);
                }
            }
        } catch (EOFException e) {
            LOG.log(Level.SEVERE, "Connection is closed");
            getMetricRegistry().counter(MetricRegistry.name(MockDatabase.class, "other-error")).inc();
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Socket error", e);
            getMetricRegistry().counter(MetricRegistry.name(MockDatabase.class, "other-error")).inc();
        }
    }

    public long getDelayExecution() {
        return delayExecution;
    }

    public void setDelayExecution(long delayExecution) {
        this.delayExecution = delayExecution;
    }

    public long getDelayResultSetNext() {
        return delayResultSetNext;
    }

    public void setDelayResultSetNext(long delayResultSetNext) {
        this.delayResultSetNext = delayResultSetNext;
    }

    public String getDataDirectory() {
        return dataDirectory;
    }

    public void setDataDirectory(String dataDirectory) {
        this.dataDirectory = dataDirectory;
    }
}
