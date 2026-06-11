package io.github.nilsfjp.ideophonearena.dto;

import java.util.List;

public class AdminStatsResponse {

    private AdminTotalsResponse totals;
    private List<AdminConditionStatsResponse> byCondition;
    private List<AdminModalityStatsResponse> byModality;

    public AdminStatsResponse(AdminTotalsResponse totals, List<AdminConditionStatsResponse> byCondition,
            List<AdminModalityStatsResponse> byModality) {
        this.totals = totals;
        this.byCondition = byCondition;
        this.byModality = byModality;
    }

    public AdminTotalsResponse getTotals() {
        return totals;
    }

    public List<AdminConditionStatsResponse> getByCondition() {
        return byCondition;
    }

    public List<AdminModalityStatsResponse> getByModality() {
        return byModality;
    }
}
