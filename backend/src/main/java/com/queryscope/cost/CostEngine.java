package com.queryscope.cost;

import com.queryscope.model.FilterPredicate;
import com.queryscope.model.IndexRef;
import com.queryscope.model.JoinSpec;
import com.queryscope.model.PlanNode;
import com.queryscope.model.QueryModel;
import com.queryscope.model.TableDef;

import java.util.List;
import java.util.Map;

import static com.queryscope.cost.CostConstants.*;

/**
 * Builds an estimated execution plan tree for a {@link QueryModel} using
 * PostgreSQL-inspired heuristic cost formulas. No cost-based join reordering
 * is attempted: joins are applied in FROM/JOIN order (left-deep plan), which
 * keeps the plan fully explainable at the cost of not finding the globally
 * cheapest join order — an explicit, documented MVP simplification.
 */
public class CostEngine {

    public PlanNode buildPlan(QueryModel model, Map<String, TableDef> tables,
                               Map<String, Double> rowCounts, List<IndexRef> indexedColumns) {

        // 1. Base scan node per table referenced in the FROM/JOIN list.
        java.util.Map<String, PlanNode> scanNodes = new java.util.LinkedHashMap<>();
        for (String table : model.getFromTables()) {
            double totalRows = rowCounts.getOrDefault(table, 1000.0);
            List<FilterPredicate> tableFilters = model.getFilters().stream()
                    .filter(f -> table.equalsIgnoreCase(f.getTable()))
                    .toList();
            scanNodes.put(table, buildScanNode(table, totalRows, tableFilters, indexedColumns));
        }

        // 2. Left-deep join tree, applied in the order joins were declared.
        String drivingTable = model.getFromTables().get(0);
        PlanNode current = scanNodes.get(drivingTable);

        for (JoinSpec join : model.getJoins()) {
            PlanNode innerNode = scanNodes.get(join.getRightTable());
            boolean innerIndexed = isIndexed(indexedColumns, join.getRightTable(), join.getRightColumn());
            current = buildJoinNode(current, innerNode, join, innerIndexed);
        }

        // 3. Optional sort on top.
        if (!model.getOrderByColumns().isEmpty()) {
            current = buildSortNode(current);
        }
        return current;
    }

    private PlanNode buildScanNode(String table, double totalRows, List<FilterPredicate> filters,
                                    List<IndexRef> indexedColumns) {
        FilterPredicate equality = filters.stream().filter(FilterPredicate::isEquality).findFirst().orElse(null);
        long rangePredicates = filters.stream().filter(f -> !f.isEquality()).count();

        double outputRows;
        if (equality != null) {
            outputRows = EQUALITY_SELECTIVITY_ROWS;
        } else if (rangePredicates > 0) {
            outputRows = Math.max(1.0, totalRows * Math.pow(RANGE_SELECTIVITY_FRACTION, rangePredicates));
        } else {
            outputRows = totalRows;
        }

        double seqScanCost = pages(totalRows) * SEQ_PAGE_COST + totalRows * CPU_TUPLE_COST;

        FilterPredicate indexablePredicate = filters.stream()
                .filter(f -> isIndexed(indexedColumns, table, f.getColumn()))
                .findFirst()
                .orElse(null);

        if (indexablePredicate == null) {
            return new PlanNode("SeqScan", "Seq Scan on " + table, outputRows, seqScanCost);
        }

        double matchedRows = indexablePredicate.isEquality()
                ? EQUALITY_SELECTIVITY_ROWS
                : Math.max(1.0, totalRows * RANGE_SELECTIVITY_FRACTION);
        double indexScanCost = pages(matchedRows) * RANDOM_PAGE_COST
                + matchedRows * (CPU_INDEX_TUPLE_COST + CPU_TUPLE_COST);

        if (indexScanCost < seqScanCost) {
            String label = "Index Scan on " + table + " (" + indexablePredicate.getColumn() + ")";
            return new PlanNode("IndexScan", label, outputRows, indexScanCost);
        }
        return new PlanNode("SeqScan", "Seq Scan on " + table, outputRows, seqScanCost);
    }

    private PlanNode buildJoinNode(PlanNode outer, PlanNode inner, JoinSpec join, boolean innerIndexed) {
        double outerRows = outer.getEstimatedRows();
        double innerRows = inner.getEstimatedRows();

        double perProbeCost = innerIndexed
                ? 2 * RANDOM_PAGE_COST + CPU_INDEX_TUPLE_COST + CPU_TUPLE_COST
                : inner.getCost();
        double nestedLoopCost = outer.getCost() + outerRows * perProbeCost;

        double hashJoinCost = outer.getCost() + inner.getCost() + (outerRows + innerRows) * CPU_OPERATOR_COST;

        double resultRows = Math.max(1.0, Math.min(outerRows, innerRows));
        String joinLabel = join.getJoinType() + " Join: " + join.getLeftTable() + " ⋈ " + join.getRightTable();

        PlanNode joinNode;
        if (hashJoinCost <= nestedLoopCost) {
            joinNode = new PlanNode("HashJoin", "Hash " + joinLabel, resultRows, hashJoinCost);
        } else {
            joinNode = new PlanNode("NestedLoop", "Nested Loop " + joinLabel, resultRows, nestedLoopCost);
        }
        joinNode.addChild(outer);
        joinNode.addChild(inner);
        return joinNode;
    }

    private PlanNode buildSortNode(PlanNode child) {
        double rows = Math.max(2.0, child.getEstimatedRows());
        double sortCost = child.getCost() + rows * (Math.log(rows) / Math.log(2)) * CPU_OPERATOR_COST;
        PlanNode sortNode = new PlanNode("Sort", "Sort", child.getEstimatedRows(), sortCost);
        sortNode.addChild(child);
        return sortNode;
    }

    private boolean isIndexed(List<IndexRef> indexedColumns, String table, String column) {
        return indexedColumns.stream().anyMatch(ix -> ix.matches(table, column));
    }
}
