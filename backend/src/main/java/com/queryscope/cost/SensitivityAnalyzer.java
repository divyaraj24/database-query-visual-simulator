package com.queryscope.cost;

import com.queryscope.model.FilterPredicate;
import com.queryscope.model.JoinSpec;
import com.queryscope.model.QueryModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import static com.queryscope.cost.CostConstants.*;

/**
 * Sensitivity / crossover analysis: instead of asking "is this hypothetical
 * index worth it under our assumed selectivity," this asks "at what
 * selectivity would it become worth it." Turns the flat-selectivity
 * heuristic (the project's most-cited limitation) into a boundary
 * condition the user can see directly, rather than a single fixed guess.
 */
public class SensitivityAnalyzer {

    public static class ScanCrossover {
        public final String table;
        public final String column;
        public final double totalRows;
        public final Double crossoverSelectivityPercent; // null if index never helps (or always helps)
        public final boolean indexAlwaysCheaper;

        public ScanCrossover(String table, String column, double totalRows,
                              Double crossoverSelectivityPercent, boolean indexAlwaysCheaper) {
            this.table = table;
            this.column = column;
            this.totalRows = totalRows;
            this.crossoverSelectivityPercent = crossoverSelectivityPercent;
            this.indexAlwaysCheaper = indexAlwaysCheaper;
        }

        public String getTable() {
            return table;
        }

        public String getColumn() {
            return column;
        }

        public double getTotalRows() {
            return totalRows;
        }

        public Double getCrossoverSelectivityPercent() {
            return crossoverSelectivityPercent;
        }

        public boolean isIndexAlwaysCheaper() {
            return indexAlwaysCheaper;
        }
    }

    public static class JoinCrossover {
        public final String outerTable;
        public final String innerTable;
        public final double innerRows;
        public final Double crossoverOuterRows; // null if one strategy always wins in the tested range

        public JoinCrossover(String outerTable, String innerTable, double innerRows, Double crossoverOuterRows) {
            this.outerTable = outerTable;
            this.innerTable = innerTable;
            this.innerRows = innerRows;
            this.crossoverOuterRows = crossoverOuterRows;
        }

        public String getOuterTable() {
            return outerTable;
        }

        public String getInnerTable() {
            return innerTable;
        }

        public double getInnerRows() {
            return innerRows;
        }

        public Double getCrossoverOuterRows() {
            return crossoverOuterRows;
        }
    }

    private static final double MIN_FRACTION = 0.0001;
    private static final double MAX_FRACTION = 1.0;
    private static final int BISECTION_STEPS = 40;

    public List<ScanCrossover> scanCrossovers(QueryModel model, Map<String, Double> rowCounts) {
        List<ScanCrossover> results = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (FilterPredicate f : model.getFilters()) {
            String key = f.getTable().toLowerCase() + "." + f.getColumn().toLowerCase();
            if (seen.contains(key)) {
                continue;
            }
            seen.add(key);

            double totalRows = rowCounts.getOrDefault(f.getTable(), 1000.0);
            double seqScanCost = pages(totalRows) * SEQ_PAGE_COST + totalRows * CPU_TUPLE_COST;

            OptionalDouble crossover = findScanCrossoverFraction(totalRows, seqScanCost);
            if (crossover.isPresent()) {
                results.add(new ScanCrossover(f.getTable(), f.getColumn(), totalRows,
                        crossover.getAsDouble() * 100.0, false));
            } else {
                // No crossover in (0,1]: index cost at fraction=1 is still below seq cost -> always cheaper,
                // or index cost at the smallest fraction is already above seq cost -> never cheaper.
                double costAtMin = indexScanCost(MIN_FRACTION, totalRows);
                boolean alwaysCheaper = costAtMin < seqScanCost;
                results.add(new ScanCrossover(f.getTable(), f.getColumn(), totalRows, null, alwaysCheaper));
            }
        }
        return results;
    }

    public List<JoinCrossover> joinCrossovers(QueryModel model, Map<String, Double> rowCounts) {
        List<JoinCrossover> results = new ArrayList<>();
        for (JoinSpec join : model.getJoins()) {
            double innerRows = rowCounts.getOrDefault(join.getRightTable(), 1000.0);
            double innerCost = pages(innerRows) * SEQ_PAGE_COST + innerRows * CPU_TUPLE_COST;
            double perProbeCostIndexed = 2 * RANDOM_PAGE_COST + CPU_INDEX_TUPLE_COST + CPU_TUPLE_COST;

            OptionalDouble crossover = findJoinCrossoverOuterRows(innerRows, innerCost, perProbeCostIndexed);
            results.add(new JoinCrossover(join.getLeftTable(), join.getRightTable(), innerRows,
                    crossover.isPresent() ? crossover.getAsDouble() : null));
        }
        return results;
    }

    private double indexScanCost(double fraction, double totalRows) {
        double matchedRows = Math.max(1.0, fraction * totalRows);
        return pages(matchedRows) * RANDOM_PAGE_COST + matchedRows * (CPU_INDEX_TUPLE_COST + CPU_TUPLE_COST);
    }

    /** Binary search for the selectivity fraction where index-scan cost first reaches seq-scan cost. */
    private OptionalDouble findScanCrossoverFraction(double totalRows, double seqScanCost) {
        double costAtMin = indexScanCost(MIN_FRACTION, totalRows);
        double costAtMax = indexScanCost(MAX_FRACTION, totalRows);

        if (costAtMin >= seqScanCost || costAtMax <= seqScanCost) {
            return OptionalDouble.empty();
        }

        double lo = MIN_FRACTION;
        double hi = MAX_FRACTION;
        for (int i = 0; i < BISECTION_STEPS; i++) {
            double mid = (lo + hi) / 2.0;
            if (indexScanCost(mid, totalRows) < seqScanCost) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return OptionalDouble.of((lo + hi) / 2.0);
    }

    private double nestedLoopCost(double outerRows, double perProbeCost) {
        double outerScanCost = pages(outerRows) * SEQ_PAGE_COST + outerRows * CPU_TUPLE_COST;
        return outerScanCost + outerRows * perProbeCost;
    }

    private double hashJoinCost(double outerRows, double innerRows, double innerCost) {
        double outerScanCost = pages(outerRows) * SEQ_PAGE_COST + outerRows * CPU_TUPLE_COST;
        return outerScanCost + innerCost + (outerRows + innerRows) * CPU_OPERATOR_COST;
    }

    /** Binary search over outer-table row count for where indexed Nested Loop stops beating Hash Join. */
    private OptionalDouble findJoinCrossoverOuterRows(double innerRows, double innerCost, double perProbeCostIndexed) {
        double lo = 1.0;
        double hi = Math.max(innerRows * 10, 1_000_000.0);

        double nlAtLo = nestedLoopCost(lo, perProbeCostIndexed);
        double hashAtLo = hashJoinCost(lo, innerRows, innerCost);
        double nlAtHi = nestedLoopCost(hi, perProbeCostIndexed);
        double hashAtHi = hashJoinCost(hi, innerRows, innerCost);

        boolean nlCheaperAtLo = nlAtLo < hashAtLo;
        boolean nlCheaperAtHi = nlAtHi < hashAtHi;
        if (nlCheaperAtLo == nlCheaperAtHi) {
            return OptionalDouble.empty();
        }

        for (int i = 0; i < BISECTION_STEPS; i++) {
            double mid = (lo + hi) / 2.0;
            boolean nlCheaperAtMid = nestedLoopCost(mid, perProbeCostIndexed) < hashJoinCost(mid, innerRows, innerCost);
            if (nlCheaperAtMid == nlCheaperAtLo) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return OptionalDouble.of((lo + hi) / 2.0);
    }
}
