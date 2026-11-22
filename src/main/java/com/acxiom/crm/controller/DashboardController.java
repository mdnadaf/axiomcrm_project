package com.acxiom.crm.controller;

import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/dashboard")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                            @RequestParam(required = false, defaultValue = "THIS_MONTH") String dateFilter,
                            Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var dashboard = dashboardService.getDashboard(principal, dateFilter);
        model.addAttribute("dashboard", dashboard);
        model.addAttribute("dateFilter", dateFilter);
        model.addAttribute("scopeUser", scopeUser);
        return "dashboard/index";
    }

    @GetMapping("/chart-data")
    @ResponseBody
    public Map<String, Object> chartData(
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
            @RequestParam String type) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        return switch (type) {
            case "leadStatus" -> dashboardService.getLeadStatusChartData(scopeUser);
            case "pipelineStage" -> dashboardService.getPipelineStageChartData(scopeUser);
            case "monthlySales" -> dashboardService.getMonthlySalesChartData(scopeUser);
            default -> Map.of("error", "Unknown chart type: " + type);
        };
    }
}
