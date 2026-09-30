package com.queryscope.api;

import com.queryscope.cost.SensitivityAnalyzer.JoinCrossover;
import com.queryscope.cost.SensitivityAnalyzer.ScanCrossover;

import java.util.List;

public class SensitivityResponse {
    private final List<ScanCrossover> scanCrossovers;
    private final List<JoinCrossover> joinCrossovers;

    public SensitivityResponse(List<ScanCrossover> scanCrossovers, List<JoinCrossover> joinCrossovers) {
        this.scanCrossovers = scanCrossovers;
        this.joinCrossovers = joinCrossovers;
    }

    public List<ScanCrossover> getScanCrossovers() {
        return scanCrossovers;
    }

    public List<JoinCrossover> getJoinCrossovers() {
        return joinCrossovers;
    }
}
