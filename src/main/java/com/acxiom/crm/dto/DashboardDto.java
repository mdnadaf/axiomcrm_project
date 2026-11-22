package com.acxiom.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDto {

    private long totalCustomers;
    private long totalLeads;
    private long openLeads;
    private long totalOpportunities;
    private long openOpportunities;
    private long wonOpportunities;
    private long lostOpportunities;

    private BigDecimal totalPipelineValue;
    private BigDecimal weightedPipelineValue;

    private Map<String, Long> leadStatusChart;
    private Map<String, Object[]> opportunityStageChart;
    private Map<Integer, BigDecimal> monthlySalesChart;

    /** TODAY / THIS_WEEK / THIS_MONTH / CUSTOM */
    private String dateFilter;
}
