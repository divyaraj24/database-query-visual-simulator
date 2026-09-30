package com.queryscope.model;

public class IndexRef {
    private String table;
    private String column;

    public IndexRef() {
    }

    public IndexRef(String table, String column) {
        this.table = table;
        this.column = column;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public boolean matches(String table, String column) {
        return this.table.equalsIgnoreCase(table) && this.column.equalsIgnoreCase(column);
    }
}
