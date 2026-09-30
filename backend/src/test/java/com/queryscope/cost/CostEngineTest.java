package com.queryscope.cost;

import com.queryscope.model.IndexRef;
import com.queryscope.model.PlanNode;
import com.queryscope.model.QueryModel;
import com.queryscope.model.TableDef;
import com.queryscope.parser.ParserFacade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hand-computed cost scenarios (see docs draft "Results and Discussion" for
 * the worked-out formulas these assert against). Each scenario doubles as
 * evidence that the ANTLR parser, the domain model, and the cost engine
 * agree with each other end to end.
 */
class CostEngineTest {

    private final CostEngine engine = new CostEngine();

    @Test
    void seqScanOnUnfilteredTable() {
        Map<String, TableDef> tables = ParserFacade.parseSchema(
                "CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(50));");
        QueryModel model = ParserFacade.parseQuery("SELECT * FROM customers");

        PlanNode plan = engine.buildPlan(model, tables, Map.of("customers", 10000.0), List.of());

        assertEquals("SeqScan", plan.getType());
        assertEquals(200.0, plan.getCost(), 0.001);
        assertEquals(10000.0, plan.getEstimatedRows(), 0.001);
    }

    @Test
    void equalityFilterSwitchesToIndexScanWhenIndexed() {
        Map<String, TableDef> tables = ParserFacade.parseSchema(
                "CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(50), city VARCHAR(50));");
        QueryModel model = ParserFacade.parseQuery("SELECT * FROM customers WHERE city = 'Chennai'");
        Map<String, Double> rowCounts = Map.of("customers", 10000.0);

        PlanNode baseline = engine.buildPlan(model, tables, rowCounts, List.of());
        assertEquals("SeqScan", baseline.getType());
        assertEquals(200.0, baseline.getCost(), 0.001);

        PlanNode withIndex = engine.buildPlan(model, tables, rowCounts,
                List.of(new IndexRef("customers", "city")));
        assertEquals("IndexScan", withIndex.getType());
        assertEquals(4.015, withIndex.getCost(), 0.001);
        assertTrue(withIndex.getCost() < baseline.getCost());
    }

    @Test
    void largeUnindexedJoinPicksHashJoin() {
        Map<String, TableDef> tables = ParserFacade.parseSchema(
                "CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(50));"
                        + "CREATE TABLE orders (id INT PRIMARY KEY, customer_id INT, amount INT);");
        QueryModel model = ParserFacade.parseQuery(
                "SELECT * FROM customers JOIN orders ON customers.id = orders.customer_id");
        Map<String, Double> rowCounts = Map.of("customers", 10000.0, "orders", 100000.0);

        PlanNode plan = engine.buildPlan(model, tables, rowCounts, List.of());

        assertEquals("HashJoin", plan.getType());
        assertEquals(2475.0, plan.getCost(), 0.01);
    }

    @Test
    void indexingLargeInnerTableSwitchesHashJoinToIndexNestedLoop() {
        Map<String, TableDef> tables = ParserFacade.parseSchema(
                "CREATE TABLE small_table (id INT PRIMARY KEY);"
                        + "CREATE TABLE big_table (id INT PRIMARY KEY, small_id INT);");
        QueryModel model = ParserFacade.parseQuery(
                "SELECT * FROM small_table JOIN big_table ON small_table.id = big_table.small_id");
        Map<String, Double> rowCounts = Map.of("small_table", 100.0, "big_table", 100000.0);

        PlanNode baseline = engine.buildPlan(model, tables, rowCounts, List.of());
        assertEquals("HashJoin", baseline.getType());
        assertEquals(2252.25, baseline.getCost(), 0.01);

        PlanNode withIndex = engine.buildPlan(model, tables, rowCounts,
                List.of(new IndexRef("big_table", "small_id")));
        assertEquals("NestedLoop", withIndex.getType());
        assertEquals(803.5, withIndex.getCost(), 0.01);

        double percentImprovement = 100.0 * (baseline.getCost() - withIndex.getCost()) / baseline.getCost();
        assertTrue(percentImprovement > 60.0 && percentImprovement < 65.0,
                "expected ~64% improvement, got " + percentImprovement);
    }
}
