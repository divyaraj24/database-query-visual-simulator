package com.queryscope.model;

public class JoinSpec {
    private final String leftTable;
    private final String rightTable;
    private final String leftColumn;
    private final String rightColumn;
    private final String joinType; // INNER, LEFT, RIGHT

    public JoinSpec(String leftTable, String rightTable, String leftColumn, String rightColumn, String joinType) {
        this.leftTable = leftTable;
        this.rightTable = rightTable;
        this.leftColumn = leftColumn;
        this.rightColumn = rightColumn;
        this.joinType = joinType;
    }

    public String getLeftTable() {
        return leftTable;
    }

    public String getRightTable() {
        return rightTable;
    }

    public String getLeftColumn() {
        return leftColumn;
    }

    public String getRightColumn() {
        return rightColumn;
    }

    public String getJoinType() {
        return joinType;
    }
}
