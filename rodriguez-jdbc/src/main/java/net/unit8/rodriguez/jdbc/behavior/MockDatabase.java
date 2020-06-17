package net.unit8.rodriguez.jdbc.behavior;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import net.unit8.rodriguez.SocketInstabilityBehavior;
import net.unit8.rodriguez.jdbc.JDBCCommand;
import net.unit8.rodriguez.jdbc.SQLStatement;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MockDatabase implements SocketInstabilityBehavior {
    private final CsvMapper mapper = CsvMapper.builder().build();
    private final CsvSchema schema = CsvSchema.builder()
            .setUseHeader(true)
            .build();

    private static class StatementSession {
        private MappingIterator<Map<String, Object>> iterator;
        private BufferedReader reader;
        private List<String> columns;
    }

    private StatementSession execute(DataInputStream in, DataOutputStream out) throws IOException{
        SQLStatement sqlStmt = new SQLStatement(in.readUTF());
        StatementSession session = new StatementSession();
        session.reader = sqlStmt.createFixtureReader();
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
            }
        });
        session.columns = sqlStmt.getColumns();

        return session;
    }

    private void next(StatementSession session, DataInputStream in, DataOutputStream out) throws IOException {
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
            StatementSession session = null;
            while(!Thread.interrupted()) {
                JDBCCommand command = JDBCCommand.values()[is.readInt()];

                switch (command) {
                    case EXECUTE: {
                        if (session != null && session.reader != null) {
                            try {
                                session.reader.close();
                            } catch (IOException ignore) {
                            }
                        }
                        session = execute(is, os);
                        break;
                    }
                    case RS_NEXT: {
                        next(Objects.requireNonNull(session), is, os);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
