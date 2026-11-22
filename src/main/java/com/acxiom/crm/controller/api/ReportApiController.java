package com.acxiom.crm.controller.api;

import com.acxiom.crm.entity.User;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@SecurityRequirement(name = "BearerAuth")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
@RequiredArgsConstructor
@Tag(name = "Report API", description = "Endpoints for report analytics and statistics")
public class ReportApiController {

    private final ReportService reportService;
    private final ScopeHelper scopeHelper;

    @GetMapping("/pipeline")
    @Operation(summary = "Get pipeline report stage-wise and owner-wise analytics")
    public ResponseEntity<Map<String, Object>> getPipelineReport(
            @AuthenticationPrincipal CrmUserDetailsService.CrmUserPrincipal principal) {
        User scopeUser = scopeHelper.getScopeUser(principal);
        Map<String, Object> report = reportService.getPipelineReport(scopeUser);
        return ResponseEntity.ok(report);
    }
}
