package com.queryscope.api;

import com.queryscope.cost.IndexRecommender.Recommendation;

import java.util.List;

public class RecommendResponse {
    private final double baselineCost;
    private final List<Recommendation> recommendations;

    public RecommendResponse(double baselineCost, List<Recommendation> recommendations) {
        this.baselineCost = baselineCost;
        this.recommendations = recommendations;
    }

    public double getBaselineCost() {
        return baselineCost;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }
}
