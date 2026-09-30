package com.queryscope.model;

public class FilterPredicate {
    private final String table;
    private final String column;
    private final String operator; // =, <>, <, <=, >, >=

    public FilterPredicate(String table, String column, String operator) {
        this.table = table;
        this.column = column;
        this.operator = operator;
    }

    public String getTable() {
        return table;
    }

    public String getColumn() {
        return column;
    }

    public String getOperator() {
        return operator;
    }

    public boolean isEquality() {
        return "=".equals(operator);
    }
}
