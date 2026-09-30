package com.queryscope.api;

import com.queryscope.cost.CostEngine;
import com.queryscope.cost.IndexRecommender;
import com.queryscope.cost.SensitivityAnalyzer;
import com.queryscope.model.IndexRef;
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
import java.util.List;
import java.util.Map;

/**
 * The single MVP endpoint: parse a hypothetical schema + query, estimate a
 * baseline execution plan, then re-estimate with the requested hypothetical
 * indexes applied, so the frontend can render and diff both plans.
 */
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

        Map<String, Double> rowCounts = request.getTableStats() == null ? Map.of() : request.getTableStats();
        List<IndexRef> requestedIndexes = request.getHypotheticalIndexes() == null
                ? List.of() : request.getHypotheticalIndexes();

        PlanNode baselinePlan = costEngine.buildPlan(model, tables, rowCounts, Collections.emptyList());
        PlanNode withIndexPlan = costEngine.buildPlan(model, tables, rowCounts, requestedIndexes);

        return new AnalyzeResponse(baselinePlan, withIndexPlan);
    }

    /**
     * Auto-derives every WHERE-filter and join column from the query itself,
     * evaluates a hypothetical index on each one independently, and returns
     * them ranked by cost improvement, generalizing the manual what-if
     * toggle above into a ranked recommendation.
     */
    @PostMapping("/api/recommend")
    public RecommendResponse recommend(@RequestBody AnalyzeRequest request) {
        Map<String, TableDef> tables = ParserFacade.parseSchema(request.getDdl());
        QueryModel model = ParserFacade.parseQuery(request.getQuery());
        Map<String, Double> rowCounts = request.getTableStats() == null ? Map.of() : request.getTableStats();

        double baselineCost = costEngine.buildPlan(model, tables, rowCounts, Collections.emptyList()).getCost();
        List<IndexRecommender.Recommendation> recommendations = indexRecommender.recommend(model, tables, rowCounts);

        return new RecommendResponse(baselineCost, recommendations);
    }

    /**
     * For every filter column, finds the selectivity at which an index would
     * start beating a sequential scan; for every join, finds the outer-table
     * row count at which an indexed nested loop stops beating a hash join.
     * Turns the flat-selectivity heuristic (the project's most-cited
     * limitation) into a boundary condition instead of a single fixed guess.
     */
    @PostMapping("/api/sensitivity")
    public SensitivityResponse sensitivity(@RequestBody AnalyzeRequest request) {
        QueryModel model = ParserFacade.parseQuery(request.getQuery());
        Map<String, Double> rowCounts = request.getTableStats() == null ? Map.of() : request.getTableStats();

        return new SensitivityResponse(
                sensitivityAnalyzer.scanCrossovers(model, rowCounts),
                sensitivityAnalyzer.joinCrossovers(model, rowCounts));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleBadInput(IllegalArgumentException ex) {
        return Map.of("error", ex.getMessage());
    }
}
