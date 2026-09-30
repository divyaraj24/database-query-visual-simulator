package com.queryscope.cost;

import com.queryscope.model.FilterPredicate;
import com.queryscope.model.IndexRef;
import com.queryscope.model.JoinSpec;
import com.queryscope.model.QueryModel;
import com.queryscope.model.TableDef;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Automatic what-if index recommendation: candidate (table, column) pairs
 * are derived directly from the query itself (every WHERE-filter column and
 * every join column), rather than requiring the user to name a column by
 * hand. Each candidate is evaluated independently against the baseline
 * (same cost engine, same formulas as the manual toggle) and ranked by cost
 * improvement, generalizing the base paper's single-index what-if check
 * into a ranked recommendation over the whole candidate set.
 */
public class IndexRecommender {

    private final CostEngine costEngine = new CostEngine();

    public static class Recommendation {
        public final String table;
        public final String column;
        public final double withIndexCost;
        public final double percentImprovement;

        public Recommendation(String table, String column, double withIndexCost, double percentImprovement) {
            this.table = table;
            this.column = column;
            this.withIndexCost = withIndexCost;
            this.percentImprovement = percentImprovement;
        }

        public String getTable() {
            return table;
        }

        public String getColumn() {
            return column;
        }

        public double getWithIndexCost() {
            return withIndexCost;
        }

        public double getPercentImprovement() {
            return percentImprovement;
        }
    }

    public List<Recommendation> recommend(QueryModel model, Map<String, TableDef> tables, Map<String, Double> rowCounts) {
        double baselineCost = costEngine.buildPlan(model, tables, rowCounts, List.of()).getCost();

        List<Recommendation> results = new ArrayList<>();
        for (IndexRef candidate : candidateColumns(model)) {
            double withIndexCost = costEngine.buildPlan(model, tables, rowCounts, List.of(candidate)).getCost();
            double improvement = baselineCost == 0 ? 0 : 100.0 * (baselineCost - withIndexCost) / baselineCost;
            results.add(new Recommendation(candidate.getTable(), candidate.getColumn(), withIndexCost, improvement));
        }

        results.sort((a, b) -> Double.compare(b.percentImprovement, a.percentImprovement));
        return results;
    }

    /** Every column a WHERE clause filters on, plus every column either side of a join. */
    private List<IndexRef> candidateColumns(QueryModel model) {
        Set<String> seen = new LinkedHashSet<>();
        List<IndexRef> candidates = new ArrayList<>();

        for (FilterPredicate f : model.getFilters()) {
            addIfNew(candidates, seen, f.getTable(), f.getColumn());
        }
        for (JoinSpec j : model.getJoins()) {
            addIfNew(candidates, seen, j.getLeftTable(), j.getLeftColumn());
            addIfNew(candidates, seen, j.getRightTable(), j.getRightColumn());
        }
        return candidates;
    }

    private void addIfNew(List<IndexRef> candidates, Set<String> seen, String table, String column) {
        String key = table.toLowerCase() + "." + column.toLowerCase();
        if (seen.add(key)) {
            candidates.add(new IndexRef(table, column));
        }
    }
}
