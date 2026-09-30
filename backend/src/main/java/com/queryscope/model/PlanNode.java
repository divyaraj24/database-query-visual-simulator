package com.queryscope.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One node of an estimated execution plan tree, shaped for direct
 * serialization into a React Flow node graph on the frontend.
 */
public class PlanNode {
    private String type;         // SeqScan | IndexScan | NestedLoop | HashJoin | Sort
    private String label;        // human-readable summary, e.g. "Seq Scan on orders"
    private double estimatedRows;
    private double cost;
    private final List<PlanNode> children = new ArrayList<>();

    public PlanNode(String type, String label, double estimatedRows, double cost) {
        this.type = type;
        this.label = label;
        this.estimatedRows = estimatedRows;
        this.cost = cost;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public double getEstimatedRows() {
        return estimatedRows;
    }

    public double getCost() {
        return cost;
    }

    public List<PlanNode> getChildren() {
        return children;
    }

    public void addChild(PlanNode child) {
        children.add(child);
    }

    public double totalCost() {
        return cost;
    }
}
