package com.queryscope.model;

import java.util.ArrayList;
import java.util.List;

public class TableDef {
    private final String name;
    private final List<String> columns = new ArrayList<>();
    private String primaryKeyColumn;

    public TableDef(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void addColumn(String column) {
        columns.add(column);
    }

    public String getPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    public void setPrimaryKeyColumn(String primaryKeyColumn) {
        this.primaryKeyColumn = primaryKeyColumn;
    }

    public boolean hasColumn(String column) {
        return columns.stream().anyMatch(c -> c.equalsIgnoreCase(column));
    }
}
