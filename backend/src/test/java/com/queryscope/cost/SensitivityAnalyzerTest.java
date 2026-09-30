package com.queryscope.cost;

import com.queryscope.model.QueryModel;
import com.queryscope.parser.ParserFacade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensitivityAnalyzerTest {

    private final SensitivityAnalyzer analyzer = new SensitivityAnalyzer();

    @Test
    void scanCrossoverMatchesHandComputedValueFor10kRows() {
        QueryModel model = ParserFacade.parseQuery("SELECT * FROM customers WHERE city = 'Chennai'");
        Map<String, Double> rowCounts = Map.of("customers", 10000.0);

        List<SensitivityAnalyzer.ScanCrossover> results = analyzer.scanCrossovers(model, rowCounts);

        assertEquals(1, results.size());
        SensitivityAnalyzer.ScanCrossover c = results.get(0);
        assertEquals("customers", c.getTable());
        assertEquals("city", c.getColumn());
        assertNotNull(c.getCrossoverSelectivityPercent());
        // Hand-derived via bisection in Python: seqScanCost=200.0, crossover at 36.0%.
        assertEquals(36.0, c.getCrossoverSelectivityPercent(), 0.5);
    }

    @Test
    void smallTableNeverBenefitsFromIndexing() {
        QueryModel model = ParserFacade.parseQuery("SELECT * FROM small_table WHERE id = 1");
        Map<String, Double> rowCounts = Map.of("small_table", 100.0);

        List<SensitivityAnalyzer.ScanCrossover> results = analyzer.scanCrossovers(model, rowCounts);

        assertEquals(1, results.size());
        SensitivityAnalyzer.ScanCrossover c = results.get(0);
        assertNull(c.getCrossoverSelectivityPercent());
        assertTrue(!c.isIndexAlwaysCheaper(), "a 100-row table's index scan floor cost should never beat its seq scan cost");
    }

    @Test
    void joinCrossoverMatchesTheKnownSmallBigJoinScenario() {
        QueryModel model = ParserFacade.parseQuery(
                "SELECT * FROM small_table JOIN big_table ON small_table.id = big_table.small_id");
        Map<String, Double> rowCounts = Map.of("small_table", 100.0, "big_table", 100000.0);

        List<SensitivityAnalyzer.JoinCrossover> results = analyzer.joinCrossovers(model, rowCounts);

        assertEquals(1, results.size());
        SensitivityAnalyzer.JoinCrossover j = results.get(0);
        assertNotNull(j.getCrossoverOuterRows());
        // At outerRows=100 (the actual scenario) indexed nested loop already wins (803.5 < 2252.25),
        // so the crossover into "hash join wins instead" must sit somewhere above 100.
        assertTrue(j.getCrossoverOuterRows() > 100.0,
                "expected crossover above the current outer size, got " + j.getCrossoverOuterRows());
    }
}
