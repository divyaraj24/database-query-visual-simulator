package com.queryscope.model;

import java.util.ArrayList;
import java.util.List;

// flattened SELECT statement; fromTables[0] is the driving table, joins are applied in order
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
