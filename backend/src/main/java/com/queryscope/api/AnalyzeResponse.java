package com.queryscope.api;

import com.queryscope.model.PlanNode;

public class AnalyzeResponse {
    private final PlanNode baselinePlan;
    private final PlanNode withIndexPlan;
    private final double baselineCost;
    private final double withIndexCost;
    private final double percentImprovement;

    public AnalyzeResponse(PlanNode baselinePlan, PlanNode withIndexPlan) {
        this.baselinePlan = baselinePlan;
        this.withIndexPlan = withIndexPlan;
        this.baselineCost = baselinePlan.totalCost();
        this.withIndexCost = withIndexPlan.totalCost();
        this.percentImprovement = baselineCost == 0
                ? 0
                : 100.0 * (baselineCost - withIndexCost) / baselineCost;
    }

    public PlanNode getBaselinePlan() {
        return baselinePlan;
    }

    public PlanNode getWithIndexPlan() {
        return withIndexPlan;
    }

    public double getBaselineCost() {
        return baselineCost;
    }

    public double getWithIndexCost() {
        return withIndexCost;
    }

    public double getPercentImprovement() {
        return percentImprovement;
    }
}
