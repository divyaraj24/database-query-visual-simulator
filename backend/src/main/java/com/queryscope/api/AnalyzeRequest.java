package com.queryscope.api;

import com.queryscope.model.IndexRef;

import java.util.List;
import java.util.Map;

public class AnalyzeRequest {
    private String ddl;
    private String query;
    private Map<String, Double> tableStats;
    private List<IndexRef> hypotheticalIndexes;

    public String getDdl() {
        return ddl;
    }

    public void setDdl(String ddl) {
        this.ddl = ddl;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, Double> getTableStats() {
        return tableStats;
    }

    public void setTableStats(Map<String, Double> tableStats) {
        this.tableStats = tableStats;
    }

    public List<IndexRef> getHypotheticalIndexes() {
        return hypotheticalIndexes;
    }

    public void setHypotheticalIndexes(List<IndexRef> hypotheticalIndexes) {
        this.hypotheticalIndexes = hypotheticalIndexes;
    }
}
