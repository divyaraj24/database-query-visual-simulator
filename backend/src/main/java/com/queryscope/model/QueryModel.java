package com.queryscope.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Flattened representation of a parsed SELECT statement, extracted by
 * {@link com.queryscope.parser.QueryModelBuilder} from the ANTLR parse tree.
 * The first entry in {@code fromTables} is the driving (left-most) table;
 * {@code joins} is applied in order to build a left-deep plan.
 */
public class QueryModel {
    private final List<String> fromTables = new ArrayList<>();
    private final List<JoinSpec> joins = new ArrayList<>();
    private final List<FilterPredicate> filters = new ArrayList<>();
    private final List<String> orderByColumns = new ArrayList<>();

    public List<String> getFromTables() {
        return fromTables;
    }

    public List<JoinSpec> getJoins() {
        return joins;
    }

    public List<FilterPredicate> getFilters() {
        return filters;
    }

    public List<String> getOrderByColumns() {
        return orderByColumns;
    }
}
