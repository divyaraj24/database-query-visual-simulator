package com.queryscope.cost;

/**
 * PostgreSQL-inspired heuristic cost constants (see postgresql.conf defaults:
 * seq_page_cost, random_page_cost, cpu_tuple_cost, cpu_index_tuple_cost,
 * cpu_operator_cost). Used as-is, not calibrated against a real optimizer —
 * the point of the MVP is a transparent, explainable cost *shape*, not
 * production-grade accuracy (documented as a limitation in the paper).
 */
public final class CostConstants {

    private CostConstants() {
    }

    public static final double SEQ_PAGE_COST = 1.0;
    public static final double RANDOM_PAGE_COST = 4.0;
    public static final double CPU_TUPLE_COST = 0.01;
    public static final double CPU_INDEX_TUPLE_COST = 0.005;
    public static final double CPU_OPERATOR_COST = 0.0025;

    /** Assumed average rows per 8 KB heap page, a standard textbook heuristic. */
    public static final double ROWS_PER_PAGE = 100.0;

    /** Flat selectivity heuristic: '=' matches ~1 row, range predicates match ~30% of the table. */
    public static final double EQUALITY_SELECTIVITY_ROWS = 1.0;
    public static final double RANGE_SELECTIVITY_FRACTION = 0.30;

    public static double pages(double rows) {
        return Math.max(1.0, Math.ceil(rows / ROWS_PER_PAGE));
    }
}
