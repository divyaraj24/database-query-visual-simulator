package com.queryscope.cost;

// PostgreSQL's default cost constants (postgresql.conf), used as-is, not calibrated
public final class CostConstants {

    private CostConstants() {
    }

    public static final double SEQ_PAGE_COST = 1.0;
    public static final double RANDOM_PAGE_COST = 4.0;
    public static final double CPU_TUPLE_COST = 0.01;
    public static final double CPU_INDEX_TUPLE_COST = 0.005;
    public static final double CPU_OPERATOR_COST = 0.0025;

    // assumed rows per 8 KB heap page
    public static final double ROWS_PER_PAGE = 100.0;

    // flat selectivity: '=' matches ~1 row, ranges match ~30% of the table
    public static final double EQUALITY_SELECTIVITY_ROWS = 1.0;
    public static final double RANGE_SELECTIVITY_FRACTION = 0.30;

    public static double pages(double rows) {
        return Math.max(1.0, Math.ceil(rows / ROWS_PER_PAGE));
    }
}
