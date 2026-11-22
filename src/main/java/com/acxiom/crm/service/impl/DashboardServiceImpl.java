package com.acxiom.crm.service.impl;

import com.acxiom.crm.dto.DashboardDto;
import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.Opportunity;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.LeadStatus;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.FollowUpRepository;
import com.acxiom.crm.repository.LeadRepository;
import com.acxiom.crm.repository.OpportunityRepository;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.security.ScopeHelper;
import com.acxiom.crm.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final OpportunityRepository opportunityRepository;
    private final FollowUpRepository followUpRepository;
    private final ScopeHelper scopeHelper;

    @Override
    @Transactional(readOnly = true)
    public DashboardDto getDashboard(CrmUserDetailsService.CrmUserPrincipal principal, String dateFilter) {
        User scopeUser = scopeHelper.getScopeUser(principal);

        List<Customer> customers = customerRepository.findAll().stream()
                .filter(c -> scopeUser == null || (c.getAssignedTo() != null && c.getAssignedTo().getId().equals(scopeUser.getId())))
                .toList();

        List<Lead> leads = leadRepository.findAll().stream()
                .filter(l -> scopeUser == null || (l.getAssignedTo() != null && l.getAssignedTo().getId().equals(scopeUser.getId())))
                .toList();

        List<Opportunity> opportunities = opportunityRepository.findAll().stream()
                .filter(o -> scopeUser == null || (o.getAssignedTo() != null && o.getAssignedTo().getId().equals(scopeUser.getId())))
                .toList();

        long totalCustomers = customers.size();
        long totalLeads = leads.size();
        long openLeads = leads.stream()
                .filter(l -> l.getStatus() == LeadStatus.NEW || l.getStatus() == LeadStatus.CONTACTED || l.getStatus() == LeadStatus.QUALIFIED)
                .count();

        long totalOpportunities = opportunities.size();
        long openOpportunities = opportunities.stream().filter(o -> o.getStatus() == OpportunityStatus.OPEN).count();
        long wonOpportunities = opportunities.stream().filter(o -> o.getStatus() == OpportunityStatus.WON).count();
        long lostOpportunities = opportunities.stream().filter(o -> o.getStatus() == OpportunityStatus.LOST).count();

        BigDecimal totalPipelineValue = opportunities.stream()
                .filter(o -> o.getStatus() == OpportunityStatus.OPEN && o.getAmount() != null)
                .map(Opportunity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal weightedPipelineValue = opportunities.stream()
                .filter(o -> o.getStatus() == OpportunityStatus.OPEN && o.getWeightedValue() != null)
                .map(Opportunity::getWeightedValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardDto.builder()
                .totalCustomers(totalCustomers)
                .totalLeads(totalLeads)
                .openLeads(openLeads)
                .totalOpportunities(totalOpportunities)
                .openOpportunities(openOpportunities)
                .wonOpportunities(wonOpportunities)
                .lostOpportunities(lostOpportunities)
                .totalPipelineValue(totalPipelineValue)
                .weightedPipelineValue(weightedPipelineValue)
                .dateFilter(dateFilter != null ? dateFilter : "THIS_MONTH")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getLeadStatusChartData(User scopeUser) {
        List<Lead> leads = leadRepository.findAll().stream()
                .filter(l -> scopeUser == null || (l.getAssignedTo() != null && l.getAssignedTo().getId().equals(scopeUser.getId())))
                .toList();

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (LeadStatus status : LeadStatus.values()) {
            long count = leads.stream().filter(l -> l.getStatus() == status).count();
            statusCounts.put(status.name(), count);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", new ArrayList<>(statusCounts.keySet()));
        result.put("data", new ArrayList<>(statusCounts.values()));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getPipelineStageChartData(User scopeUser) {
        List<Opportunity> opps = opportunityRepository.findAll().stream()
                .filter(o -> scopeUser == null || (o.getAssignedTo() != null && o.getAssignedTo().getId().equals(scopeUser.getId())))
                .toList();

        List<String> labels = new ArrayList<>();
        List<Long> counts = new ArrayList<>();
        List<BigDecimal> amounts = new ArrayList<>();

        for (OpportunityStage stage : OpportunityStage.values()) {
            labels.add(stage.name());
            List<Opportunity> stageOpps = opps.stream().filter(o -> o.getStage() == stage).toList();
            counts.add((long) stageOpps.size());
            BigDecimal sum = stageOpps.stream()
                    .map(Opportunity::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            amounts.add(sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("counts", counts);
        result.put("amounts", amounts);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getMonthlySalesChartData(User scopeUser) {
        int currentYear = LocalDate.now().getYear();
        List<Opportunity> wonOpps = opportunityRepository.findAll().stream()
                .filter(o -> o.getStatus() == OpportunityStatus.WON)
                .filter(o -> scopeUser == null || (o.getAssignedTo() != null && o.getAssignedTo().getId().equals(scopeUser.getId())))
                .filter(o -> o.getUpdatedDate() != null && o.getUpdatedDate().getYear() == currentYear)
                .toList();

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        List<BigDecimal> monthlyTotals = new ArrayList<>();

        for (int m = 1; m <= 12; m++) {
            final int month = m;
            BigDecimal sum = wonOpps.stream()
                    .filter(o -> o.getUpdatedDate().getMonthValue() == month && o.getAmount() != null)
                    .map(Opportunity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            monthlyTotals.add(sum);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", Arrays.asList(months));
        result.put("data", monthlyTotals);
        return result;
    }
}
