package com.queryscope.parser;

import com.queryscope.grammar.SqlSubsetLexer;
import com.queryscope.grammar.SqlSubsetParser;
import com.queryscope.model.QueryModel;
import com.queryscope.model.TableDef;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.Map;

/**
 * Entry point that turns raw DDL/DML text into the ANTLR parse tree and then
 * into the domain model ({@link TableDef}, {@link QueryModel}) the cost
 * engine works with. Kept deliberately thin: grammar rules do the structural
 * work, the two visitor classes do the AST-to-model mapping.
 */
public final class ParserFacade {

    private ParserFacade() {
    }

    public static Map<String, TableDef> parseSchema(String ddl) {
        SqlSubsetParser parser = newParser(ddl);
        SqlSubsetParser.ScriptContext script = parser.script();
        SchemaVisitor visitor = new SchemaVisitor();
        for (SqlSubsetParser.StatementContext stmt : script.statement()) {
            if (stmt.createTableStatement() != null) {
                visitor.visit(stmt.createTableStatement());
            }
        }
        return visitor.getTables();
    }

    public static QueryModel parseQuery(String query) {
        SqlSubsetParser parser = newParser(query);
        SqlSubsetParser.SelectStatementContext ctx = parser.selectStatement();
        QueryModelBuilder builder = new QueryModelBuilder();
        builder.visit(ctx);
        return builder.getModel();
    }

    private static SqlSubsetParser newParser(String text) {
        SqlSubsetLexer lexer = new SqlSubsetLexer(CharStreams.fromString(text));
        lexer.removeErrorListeners();
        lexer.addErrorListener(THROWING_LISTENER);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        SqlSubsetParser parser = new SqlSubsetParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(THROWING_LISTENER);
        return parser;
    }

    private static final BaseErrorListener THROWING_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                 int charPositionInLine, String msg, RecognitionException e) {
            throw new IllegalArgumentException("SQL syntax error at line " + line + ":" + charPositionInLine + " - " + msg);
        }
    };
}
