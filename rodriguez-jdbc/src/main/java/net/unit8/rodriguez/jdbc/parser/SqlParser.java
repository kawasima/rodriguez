package net.unit8.rodriguez.jdbc.parser;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.upsert.Upsert;
import net.unit8.rodriguez.jdbc.JDBCCommand;

public class SqlParser {
        public ParseResult parse(String sql) throws JSQLParserException {
            Statement stmt = CCJSqlParserUtil.parse(sql);
            ParseResult parseResult = new ParseResult();
            StatementVisitor statementVisitor = createStatementVisitor(parseResult);
            stmt.accept(statementVisitor);
            if (parseResult.getType() == null) {
                throw new IllegalArgumentException("Unsupported sql type");
            }
            return parseResult;
        }

        private SelectVisitor createSelectVisitor(final ParseResult parseResult) {
            return new SelectVisitorAdapter() {
                @Override
                public void visit(PlainSelect select) {
                    select.getSelectItems().forEach(item -> item.accept(createSelectItemVisitor(parseResult)));
                }
            };
        }

        private StatementVisitor createStatementVisitor(final ParseResult parseResult) {
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
                    select.getSelectBody().accept(createSelectVisitor(parseResult));
                }
            };
        }

        private SelectItemVisitor createSelectItemVisitor(final ParseResult parseResult) {
            return new SelectItemVisitorAdapter() {
                @Override
                public void visit(SelectExpressionItem item) {
                    if (item.getAlias() != null) {
                        parseResult.getColumns().add(item.getAlias().getName());
                    } else {
                        parseResult.getColumns().add(item.getExpression().toString());
                    }
                }
            };
        }
}
