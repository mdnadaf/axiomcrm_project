package com.acxiom.crm.service;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.FollowUp;
import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.Opportunity;
import com.acxiom.crm.entity.Activity;
import com.acxiom.crm.entity.AuditLog;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.FollowUpStatus;
import com.acxiom.crm.enums.LeadStatus;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface ReportService {

    Page<Customer> getCustomerReport(User scopeUser, String search, Boolean active, Pageable pageable);

    Page<Lead> getLeadReport(User scopeUser, String search, LeadStatus status, Pageable pageable);

    Page<Opportunity> getOpportunityReport(User scopeUser, String search, OpportunityStage stage, OpportunityStatus status, Pageable pageable);

    Page<FollowUp> getFollowUpReport(User scopeUser, FollowUpStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    Map<String, Object> getPipelineReport(User scopeUser);

    Map<String, Object> getConversionReport(User scopeUser);

    Page<Activity> getUserActivityReport(User scopeUser, Pageable pageable);

    Page<AuditLog> getAuditReport(Long userId, String entityName, String action, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    String exportCustomerCsv(User scopeUser);

    String exportLeadCsv(User scopeUser);

    String exportOpportunityCsv(User scopeUser);

    String exportFollowUpCsv(User scopeUser);

    String exportPipelineCsv(User scopeUser);

    String exportUserActivityCsv(User scopeUser);

    String exportAuditCsv(Long userId, String entityName, String action, LocalDateTime fromDate, LocalDateTime toDate);
}
