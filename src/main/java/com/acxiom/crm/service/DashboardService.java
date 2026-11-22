package com.acxiom.crm.service;

import com.acxiom.crm.dto.DashboardDto;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.security.CrmUserDetailsService;

import java.util.Map;

public interface DashboardService {

    DashboardDto getDashboard(CrmUserDetailsService.CrmUserPrincipal principal, String dateFilter);

    Map<String, Object> getLeadStatusChartData(User scopeUser);

    Map<String, Object> getPipelineStageChartData(User scopeUser);

    Map<String, Object> getMonthlySalesChartData(User scopeUser);
}
