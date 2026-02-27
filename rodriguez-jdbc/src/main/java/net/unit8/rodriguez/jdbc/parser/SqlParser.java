package net.unit8.rodriguez.jdbc.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import net.unit8.rodriguez.jdbc.JDBCCommand;

/**
 * Parses SQL statements to determine their command type and extract column information.
 */
public class SqlParser {
        /**
         * Constructs a new {@code SqlParser}.
         */
        public SqlParser() {
        }

        /**
         * Parses the given SQL string and returns a {@link ParseResult} containing the command type and columns.
         *
         * @param sql the SQL string to parse
         * @return the parse result containing the command type and column names
         * @throws JSQLParserException if the SQL cannot be parsed
         */
        public ParseResult parse(String sql) throws JSQLParserException {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            ParseResult parseResult = new ParseResult();
            stmt.accept(createStatementVisitor(parseResult));
            if (parseResult.getType() == null) {
                throw new IllegalArgumentException("Unsupported sql type");
            }
            return parseResult;
        }

        private StatementVisitorAdapter createStatementVisitor(final ParseResult parseResult) {
            return new StatementVisitorAdapter() {
                @Override
                public void visit(Update update) {
                    parseResult.setType(JDBCCommand.EXECUTE_UPDATE);
                }
                @Override
                public void visit(Insert insert) {
                    parseResult.setType(JDBCCommand.EXECUTE_UPDATE);
                }
                @Override
                public void visit(Upsert upsert) {
                    parseResult.setType(JDBCCommand.EXECUTE_UPDATE);
                }
                @Override
                public void visit(Delete delete) {
                    parseResult.setType(JDBCCommand.EXECUTE_UPDATE);
                }
                @Override
                public void visit(Select select) {
                    parseResult.setType(JDBCCommand.EXECUTE_QUERY);
                    if (select instanceof PlainSelect) {
                        PlainSelect plainSelect = (PlainSelect) select;
                        for (SelectItem<?> item : plainSelect.getSelectItems()) {
                            if (item.getAlias() != null) {
                                parseResult.getColumns().add(item.getAlias().getName());
                            } else {
                                parseResult.getColumns().add(item.getExpression().toString());
                            }
                        }
                    }
                }
            };
        }
}
