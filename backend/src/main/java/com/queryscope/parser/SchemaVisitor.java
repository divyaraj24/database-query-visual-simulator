package com.queryscope.parser;

import com.queryscope.grammar.SqlSubsetBaseVisitor;
import com.queryscope.grammar.SqlSubsetParser;
import com.queryscope.model.TableDef;

import java.util.LinkedHashMap;
import java.util.Map;

/** Builds a {@code Map<tableName, TableDef>} from CREATE TABLE statements. */
public class SchemaVisitor extends SqlSubsetBaseVisitor<Void> {

    private final Map<String, TableDef> tables = new LinkedHashMap<>();

    public Map<String, TableDef> getTables() {
        return tables;
    }

    @Override
    public Void visitCreateTableStatement(SqlSubsetParser.CreateTableStatementContext ctx) {
        String tableName = ctx.tableName().getText();
        TableDef table = new TableDef(tableName);
        for (SqlSubsetParser.ColumnDefContext columnDef : ctx.columnDef()) {
            String columnName = columnDef.columnName().getText();
            table.addColumn(columnName);
            for (SqlSubsetParser.ColumnConstraintContext constraint : columnDef.columnConstraint()) {
                if (constraint.PRIMARY() != null) {
                    table.setPrimaryKeyColumn(columnName);
                }
            }
        }
        tables.put(tableName, table);
        return null;
    }
}
