package com.acxiom.crm.controller;

import com.acxiom.crm.enums.FollowUpStatus;
import com.acxiom.crm.enums.LeadStatus;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.ReportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/reports")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ScopeHelper scopeHelper;

    @GetMapping
    public String index(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        model.addAttribute("isAdmin", scopeHelper.isAdmin(principal));
        model.addAttribute("isManager", scopeHelper.isManager(principal));
        return "report/index";
    }

    @GetMapping("/customers")
    public String customerReport(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) String search,
                                 @RequestParam(required = false) Boolean active,
                                 Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var customers = reportService.getCustomerReport(scopeUser, search, active, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("customers", customers);
        model.addAttribute("search", search);
        model.addAttribute("active", active);
        return "report/customer";
    }

    @GetMapping("/leads")
    public String leadReport(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size,
                             @RequestParam(required = false) String search,
                             @RequestParam(required = false) LeadStatus status,
                             Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var leads = reportService.getLeadReport(scopeUser, search, status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate")));
        model.addAttribute("leads", leads);
        model.addAttribute("search", search);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", LeadStatus.values());
        return "report/lead";
    }

    @GetMapping("/opportunities")
    public String opportunityReport(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                                    @RequestParam(defaultValue = "0") int page,
                                    @RequestParam(defaultValue = "20") int size,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) OpportunityStage stage,
                                    @RequestParam(required = false) OpportunityStatus status,
                                    Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var opps = reportService.getOpportunityReport(scopeUser, search, stage, status, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "amount")));
        model.addAttribute("opportunities", opps);
        model.addAttribute("search", search);
        model.addAttribute("selectedStage", stage);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("stages", OpportunityStage.values());
        model.addAttribute("statuses", OpportunityStatus.values());
        return "report/opportunity";
    }

    @GetMapping("/followups")
    public String followUpReport(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) FollowUpStatus status,
                                 @RequestParam(required = false) LocalDate fromDate,
                                 @RequestParam(required = false) LocalDate toDate,
                                 Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var followups = reportService.getFollowUpReport(scopeUser, status, fromDate, toDate, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "followUpDate")));
        model.addAttribute("followups", followups);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("fromDate", fromDate);
        model.addAttribute("toDate", toDate);
        model.addAttribute("statuses", FollowUpStatus.values());
        return "report/followup";
    }

    @GetMapping("/pipeline")
    public String pipelineReport(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var report = reportService.getPipelineReport(scopeUser);
        model.addAttribute("stageSummary", report.get("stageSummary"));
        model.addAttribute("opportunities", report.get("opportunities"));
        return "report/pipeline";
    }

    @GetMapping("/conversion")
    public String conversionReport(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal, Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var report = reportService.getConversionReport(scopeUser);
        model.addAttribute("report", report);
        return "report/conversion";
    }

    @GetMapping("/user-activity")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public String userActivityReport(@AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "20") int size,
                                     Model model) {
        var scopeUser = scopeHelper.getScopeUser(principal);
        var activities = reportService.getUserActivityReport(scopeUser, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "activityDate")));
        model.addAttribute("activities", activities);
        return "report/user-activity";
    }

    @GetMapping("/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public String auditReport(@RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size,
                              @RequestParam(required = false) Long userId,
                              @RequestParam(required = false) String entityName,
                              @RequestParam(required = false) String action,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
                              Model model) {
        var logs = reportService.getAuditReport(userId, entityName, action, fromDate, toDate, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate")));
        model.addAttribute("logs", logs);
        model.addAttribute("userId", userId);
        model.addAttribute("entityName", entityName);
        model.addAttribute("action", action);
        return "report/audit";
    }

    @GetMapping("/{type}/export")
    public void exportCsv(@PathVariable String type,
                          @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal,
                          HttpServletResponse response) throws IOException {
        var scopeUser = scopeHelper.getScopeUser(principal);
        String csv = switch (type) {
            case "customers" -> reportService.exportCustomerCsv(scopeUser);
            case "leads" -> reportService.exportLeadCsv(scopeUser);
            case "opportunities" -> reportService.exportOpportunityCsv(scopeUser);
            case "followups" -> reportService.exportFollowUpCsv(scopeUser);
            case "pipeline" -> reportService.exportPipelineCsv(scopeUser);
            case "user-activity" -> reportService.exportUserActivityCsv(scopeUser);
            case "audit" -> reportService.exportAuditCsv(null, null, null, null, null);
            default -> "Error: Unknown report type " + type;
        };

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + type + "_report.csv");
        response.getWriter().write(csv);
    }
}
