package com.queryscope.parser;

import com.queryscope.grammar.SqlSubsetBaseVisitor;
import com.queryscope.grammar.SqlSubsetParser;
import com.queryscope.model.FilterPredicate;
import com.queryscope.model.JoinSpec;
import com.queryscope.model.QueryModel;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Walks a single {@code selectStatement} parse tree and flattens it into a
 * {@link QueryModel}. Table aliases (the {@code AS x} part of a FROM/JOIN
 * item) are resolved back to their real table name so the cost engine only
 * ever has to reason about actual tables.
 */
public class QueryModelBuilder extends SqlSubsetBaseVisitor<Void> {

    private final QueryModel model = new QueryModel();
    private final Map<String, String> aliasToTable = new LinkedHashMap<>();

    public QueryModel getModel() {
        return model;
    }

    @Override
    public Void visitSelectStatement(SqlSubsetParser.SelectStatementContext ctx) {
        String driving = registerFromItem(ctx.fromItem());
        model.getFromTables().add(driving);

        for (SqlSubsetParser.JoinClauseContext joinCtx : ctx.joinClause()) {
            String rightTable = registerFromItem(joinCtx.fromItem());
            model.getFromTables().add(rightTable);

            String joinType = resolveJoinType(joinCtx.joinType());
            SqlSubsetParser.JoinConditionContext cond = joinCtx.joinCondition();
            String leftCol = resolveColumnRef(cond.qualifiedColumn(0));
            String rightCol = resolveColumnRef(cond.qualifiedColumn(1));
            String leftTable = resolveTableRef(cond.qualifiedColumn(0));
            String rightTableRef = resolveTableRef(cond.qualifiedColumn(1));

            // Normalise so the join's "right" side matches the table just introduced by this clause.
            if (rightTableRef != null && rightTableRef.equalsIgnoreCase(rightTable)) {
                model.getJoins().add(new JoinSpec(leftTable, rightTable, leftCol, rightCol, joinType));
            } else {
                model.getJoins().add(new JoinSpec(rightTableRef, rightTable, rightCol, leftCol, joinType));
            }
        }

        if (ctx.whereClause() != null) {
            for (SqlSubsetParser.PredicateContext predicateCtx : ctx.whereClause().predicate()) {
                String table = resolveTableRef(predicateCtx.qualifiedColumn());
                String column = resolveColumnRef(predicateCtx.qualifiedColumn());
                String op = predicateCtx.comparisonOp().getText();
                model.getFilters().add(new FilterPredicate(table, column, op));
            }
        }

        if (ctx.orderByClause() != null) {
            for (SqlSubsetParser.OrderItemContext item : ctx.orderByClause().orderItem()) {
                model.getOrderByColumns().add(resolveColumnRef(item.qualifiedColumn()));
            }
        }
        return null;
    }

    private String registerFromItem(SqlSubsetParser.FromItemContext ctx) {
        String tableName = ctx.tableName().getText();
        if (ctx.IDENTIFIER() != null) {
            aliasToTable.put(ctx.IDENTIFIER().getText(), tableName);
        }
        aliasToTable.put(tableName, tableName);
        return tableName;
    }

    private String resolveJoinType(SqlSubsetParser.JoinTypeContext ctx) {
        if (ctx.LEFT() != null) {
            return "LEFT";
        }
        if (ctx.RIGHT() != null) {
            return "RIGHT";
        }
        return "INNER";
    }

    /** Qualifier before the dot, resolved through the alias map; falls back to the sole FROM table. */
    private String resolveTableRef(SqlSubsetParser.QualifiedColumnContext ctx) {
        if (ctx.IDENTIFIER() != null) {
            String qualifier = ctx.IDENTIFIER().getText();
            return aliasToTable.getOrDefault(qualifier, qualifier);
        }
        if (model.getFromTables().size() == 1) {
            return model.getFromTables().get(0);
        }
        return null;
    }

    private String resolveColumnRef(SqlSubsetParser.QualifiedColumnContext ctx) {
        return ctx.columnName().getText();
    }
}
