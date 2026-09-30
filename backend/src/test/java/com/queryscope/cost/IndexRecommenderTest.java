package com.queryscope.cost;

import com.queryscope.model.QueryModel;
import com.queryscope.model.TableDef;
import com.queryscope.parser.ParserFacade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexRecommenderTest {

    private final IndexRecommender recommender = new IndexRecommender();

    @Test
    void ranksTheJoinColumnHighestForTheSmallBigJoin() {
        Map<String, TableDef> tables = ParserFacade.parseSchema(
                "CREATE TABLE small_table (id INT PRIMARY KEY);"
                        + "CREATE TABLE big_table (id INT PRIMARY KEY, small_id INT);");
        QueryModel model = ParserFacade.parseQuery(
                "SELECT * FROM small_table JOIN big_table ON small_table.id = big_table.small_id");
        Map<String, Double> rowCounts = Map.of("small_table", 100.0, "big_table", 100000.0);

        List<IndexRecommender.Recommendation> results = recommender.recommend(model, tables, rowCounts);

        // Candidates should be exactly the two join columns, deduplicated.
        assertEquals(2, results.size());

        IndexRecommender.Recommendation top = results.get(0);
        assertEquals("big_table", top.getTable());
        assertEquals("small_id", top.getColumn());
        assertEquals(803.5, top.getWithIndexCost(), 0.01);
        assertTrue(top.getPercentImprovement() > 60.0 && top.getPercentImprovement() < 65.0,
                "expected ~64% improvement, got " + top.getPercentImprovement());

        // Indexing the small_table side (the outer/driving table) shouldn't help at all here.
        IndexRecommender.Recommendation second = results.get(1);
        assertEquals("small_table", second.getTable());
        assertEquals(0.0, second.getPercentImprovement(), 0.01);
    }

    @Test
    void ranksTheEqualityFilterColumnForASingleTable() {
        Map<String, TableDef> tables = ParserFacade.parseSchema(
                "CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(50), city VARCHAR(50));");
        QueryModel model = ParserFacade.parseQuery("SELECT * FROM customers WHERE city = 'Chennai'");
        Map<String, Double> rowCounts = Map.of("customers", 10000.0);

        List<IndexRecommender.Recommendation> results = recommender.recommend(model, tables, rowCounts);

        assertEquals(1, results.size());
        assertEquals("customers", results.get(0).getTable());
        assertEquals("city", results.get(0).getColumn());
        assertEquals(4.015, results.get(0).getWithIndexCost(), 0.001);
    }
}
