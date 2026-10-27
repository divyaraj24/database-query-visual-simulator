package com.queryscope.api;

import com.queryscope.cost.CostEngine;
import com.queryscope.cost.IndexRecommender;
import com.queryscope.cost.SensitivityAnalyzer;
import com.queryscope.model.IndexRef;
import com.queryscope.model.JoinSpec;
import com.queryscope.model.PlanNode;
import com.queryscope.model.QueryModel;
import com.queryscope.model.TableDef;
import com.queryscope.parser.ParserFacade;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@CrossOrigin(origins = "*")
public class AnalyzeController {

    private final CostEngine costEngine = new CostEngine();
    private final IndexRecommender indexRecommender = new IndexRecommender();
    private final SensitivityAnalyzer sensitivityAnalyzer = new SensitivityAnalyzer();

    @PostMapping("/api/analyze")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest request) {
        Map<String, TableDef> tables = ParserFacade.parseSchema(request.getDdl());
        QueryModel model = ParserFacade.parseQuery(request.getQuery());
        validateTablesExist(model, tables);

        Map<String, Double> rowCounts = request.getTableStats() == null ? Map.of() : request.getTableStats();
        List<IndexRef> requestedIndexes = request.getHypotheticalIndexes() == null
                ? List.of() : request.getHypotheticalIndexes();

        PlanNode baselinePlan = costEngine.buildPlan(model, tables, rowCounts, Collections.emptyList());
        PlanNode withIndexPlan = costEngine.buildPlan(model, tables, rowCounts, requestedIndexes);

        return new AnalyzeResponse(baselinePlan, withIndexPlan);
    }

    // evaluates every filter/join column as a candidate index and ranks by cost improvement
    @PostMapping("/api/recommend")
    public RecommendResponse recommend(@RequestBody AnalyzeRequest request) {
        Map<String, TableDef> tables = ParserFacade.parseSchema(request.getDdl());
        QueryModel model = ParserFacade.parseQuery(request.getQuery());
        validateTablesExist(model, tables);
        Map<String, Double> rowCounts = request.getTableStats() == null ? Map.of() : request.getTableStats();

        double baselineCost = costEngine.buildPlan(model, tables, rowCounts, Collections.emptyList()).getCost();
        List<IndexRecommender.Recommendation> recommendations = indexRecommender.recommend(model, tables, rowCounts);

        return new RecommendResponse(baselineCost, recommendations);
    }

    // finds the selectivity / outer-row-count crossover where the cheaper plan would flip
    @PostMapping("/api/sensitivity")
    public SensitivityResponse sensitivity(@RequestBody AnalyzeRequest request) {
        Map<String, TableDef> tables = ParserFacade.parseSchema(request.getDdl());
        QueryModel model = ParserFacade.parseQuery(request.getQuery());
        validateTablesExist(model, tables);
        Map<String, Double> rowCounts = request.getTableStats() == null ? Map.of() : request.getTableStats();

        return new SensitivityResponse(
                sensitivityAnalyzer.scanCrossovers(model, rowCounts),
                sensitivityAnalyzer.joinCrossovers(model, rowCounts));
    }

    // catches a typo'd or undeclared table name early, instead of silently costing it with a
    // made-up 1000-row default and producing a plan for a table that doesn't exist
    private void validateTablesExist(QueryModel model, Map<String, TableDef> tables) {
        Set<String> referenced = new LinkedHashSet<>(model.getFromTables());
        for (JoinSpec join : model.getJoins()) {
            referenced.add(join.getLeftTable());
            referenced.add(join.getRightTable());
        }
        for (String table : referenced) {
            boolean declared = tables.keySet().stream().anyMatch(t -> t.equalsIgnoreCase(table));
            if (!declared) {
                throw new IllegalArgumentException(
                        "Table '" + table + "' is referenced in the query but not declared in the schema.");
            }
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadInput(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
