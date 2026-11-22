package com.acxiom.crm.service.impl;

import com.acxiom.crm.entity.*;
import com.acxiom.crm.enums.FollowUpStatus;
import com.acxiom.crm.enums.LeadStatus;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import com.acxiom.crm.repository.*;
import com.acxiom.crm.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final FollowUpRepository followUpRepository;
    private final ActivityRepository activityRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<Customer> getCustomerReport(User scopeUser, String search, Boolean active, Pageable pageable) {
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        return customerRepository.search(searchTerm, scopeUser, active, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Lead> getLeadReport(User scopeUser, String search, LeadStatus status, Pageable pageable) {
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        return leadRepository.search(searchTerm, status, scopeUser, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Opportunity> getOpportunityReport(User scopeUser, String search, OpportunityStage stage, OpportunityStatus status, Pageable pageable) {
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        return opportunityRepository.search(searchTerm, stage, status, scopeUser, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUp> getFollowUpReport(User scopeUser, FollowUpStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return followUpRepository.search(status, scopeUser, fromDate, toDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPipelineReport(User scopeUser) {
        List<Opportunity> opps = opportunityRepository.findAll().stream()
                .filter(o -> scopeUser == null || (o.getAssignedTo() != null && o.getAssignedTo().getId().equals(scopeUser.getId())))
                .toList();

        Map<String, Map<String, Object>> stageSummary = new LinkedHashMap<>();
        for (OpportunityStage stage : OpportunityStage.values()) {
            List<Opportunity> stageOpps = opps.stream().filter(o -> o.getStage() == stage).toList();
            BigDecimal totalAmount = stageOpps.stream().map(Opportunity::getAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal weightedAmount = stageOpps.stream().map(Opportunity::getWeightedValue).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

            Map<String, Object> data = new HashMap<>();
            data.put("count", stageOpps.size());
            data.put("totalAmount", totalAmount);
            data.put("weightedAmount", weightedAmount);
            stageSummary.put(stage.name(), data);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stageSummary", stageSummary);
        result.put("opportunities", opps);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getConversionReport(User scopeUser) {
        List<Lead> leads = leadRepository.findAll().stream()
                .filter(l -> scopeUser == null || (l.getAssignedTo() != null && l.getAssignedTo().getId().equals(scopeUser.getId())))
                .toList();

        long converted = leads.stream().filter(l -> l.getStatus() == LeadStatus.CONVERTED).count();
        long notConverted = leads.size() - converted;
        double rate = leads.isEmpty() ? 0.0 : ((double) converted / leads.size()) * 100.0;

        Map<String, Object> result = new HashMap<>();
        result.put("totalLeads", leads.size());
        result.put("convertedLeads", converted);
        result.put("notConvertedLeads", notConverted);
        result.put("conversionRate", Math.round(rate * 100.0) / 100.0);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> getUserActivityReport(User scopeUser, Pageable pageable) {
        return activityRepository.search(null, null, scopeUser, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAuditReport(Long userId, String entityName, String action, LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        return auditLogRepository.search(userId, entityName, action, fromDate, toDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportCustomerCsv(User scopeUser) {
        StringBuilder sb = new StringBuilder("Customer Code,Name,Email,Phone,Company,Active,Assigned To\n");
        Page<Customer> page = customerRepository.search(null, scopeUser, null, PageRequest.of(0, 1000));
        for (Customer c : page.getContent()) {
            sb.append(escape(c.getCustomerCode())).append(",")
              .append(escape(c.getName())).append(",")
              .append(escape(c.getEmail())).append(",")
              .append(escape(c.getPhone())).append(",")
              .append(escape(c.getCompany())).append(",")
              .append(c.isActive()).append(",")
              .append(escape(c.getAssignedTo() != null ? c.getAssignedTo().getFullName() : "")).append("\n");
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportLeadCsv(User scopeUser) {
        StringBuilder sb = new StringBuilder("Lead Code,Name,Email,Phone,Company,Status,Source,Expected Value,Assigned To\n");
        Page<Lead> page = leadRepository.search(null, null, scopeUser, PageRequest.of(0, 1000));
        for (Lead l : page.getContent()) {
            sb.append(escape(l.getLeadCode())).append(",")
              .append(escape(l.getName())).append(",")
              .append(escape(l.getEmail())).append(",")
              .append(escape(l.getPhone())).append(",")
              .append(escape(l.getCompany())).append(",")
              .append(l.getStatus().name()).append(",")
              .append(escape(l.getSource())).append(",")
              .append(l.getExpectedValue()).append(",")
              .append(escape(l.getAssignedTo() != null ? l.getAssignedTo().getFullName() : "")).append("\n");
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportOpportunityCsv(User scopeUser) {
        StringBuilder sb = new StringBuilder("Name,Customer,Stage,Status,Amount,Probability,Weighted Value,Expected Close Date,Assigned To\n");
        Page<Opportunity> page = opportunityRepository.search(null, null, null, scopeUser, PageRequest.of(0, 1000));
        for (Opportunity o : page.getContent()) {
            sb.append(escape(o.getName())).append(",")
              .append(escape(o.getCustomer() != null ? o.getCustomer().getName() : "")).append(",")
              .append(o.getStage().name()).append(",")
              .append(o.getStatus().name()).append(",")
              .append(o.getAmount()).append(",")
              .append(o.getProbability()).append(",")
              .append(o.getWeightedValue()).append(",")
              .append(o.getExpectedCloseDate()).append(",")
              .append(escape(o.getAssignedTo() != null ? o.getAssignedTo().getFullName() : "")).append("\n");
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportFollowUpCsv(User scopeUser) {
        StringBuilder sb = new StringBuilder("Follow-Up Date,Type,Status,Customer/Lead,Remarks,Assigned To\n");
        Page<FollowUp> page = followUpRepository.search(null, scopeUser, null, null, PageRequest.of(0, 1000));
        for (FollowUp f : page.getContent()) {
            String related = f.getCustomer() != null ? f.getCustomer().getName() : (f.getLead() != null ? f.getLead().getName() : "");
            sb.append(f.getFollowUpDate()).append(",")
              .append(f.getType().name()).append(",")
              .append(f.getStatus().name()).append(",")
              .append(escape(related)).append(",")
              .append(escape(f.getRemarks())).append(",")
              .append(escape(f.getAssignedTo() != null ? f.getAssignedTo().getFullName() : "")).append("\n");
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportPipelineCsv(User scopeUser) {
        StringBuilder sb = new StringBuilder("Stage,Count,Total Amount,Weighted Amount\n");
        Map<String, Object> report = getPipelineReport(scopeUser);
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> stageSummary = (Map<String, Map<String, Object>>) report.get("stageSummary");
        for (Map.Entry<String, Map<String, Object>> e : stageSummary.entrySet()) {
            sb.append(e.getKey()).append(",")
              .append(e.getValue().get("count")).append(",")
              .append(e.getValue().get("totalAmount")).append(",")
              .append(e.getValue().get("weightedAmount")).append("\n");
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportUserActivityCsv(User scopeUser) {
        StringBuilder sb = new StringBuilder("Date,Type,Subject,Status,Assigned To\n");
        Page<Activity> page = activityRepository.search(null, null, scopeUser, null, null, PageRequest.of(0, 1000));
        for (Activity a : page.getContent()) {
            sb.append(a.getActivityDate()).append(",")
              .append(a.getType().name()).append(",")
              .append(escape(a.getSubject())).append(",")
              .append(a.getStatus().name()).append(",")
              .append(escape(a.getAssignedTo() != null ? a.getAssignedTo().getFullName() : "")).append("\n");
        }
        return sb.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public String exportAuditCsv(Long userId, String entityName, String action, LocalDateTime fromDate, LocalDateTime toDate) {
        StringBuilder sb = new StringBuilder("Timestamp,Username,Action,Entity,Record ID,IP Address\n");
        Page<AuditLog> page = auditLogRepository.search(userId, entityName, action, fromDate, toDate, PageRequest.of(0, 1000));
        for (AuditLog a : page.getContent()) {
            sb.append(a.getCreatedDate()).append(",")
              .append(escape(a.getUsername())).append(",")
              .append(escape(a.getAction())).append(",")
              .append(escape(a.getEntityName())).append(",")
              .append(a.getRecordId()).append(",")
              .append(escape(a.getIpAddress())).append("\n");
        }
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
