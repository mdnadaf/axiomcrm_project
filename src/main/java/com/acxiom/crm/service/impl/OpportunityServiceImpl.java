package com.acxiom.crm.service.impl;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.Opportunity;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.enums.OpportunityStatus;
import com.acxiom.crm.exception.BusinessException;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.LeadRepository;
import com.acxiom.crm.repository.OpportunityRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.service.AuditLogService;
import com.acxiom.crm.service.OpportunityService;
import com.acxiom.crm.viewmodel.OpportunityForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpportunityServiceImpl implements OpportunityService {

    private final OpportunityRepository opportunityRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<Opportunity> listOpportunities(User scopeUser, String search, OpportunityStage stage, OpportunityStatus status, Pageable pageable) {
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        return opportunityRepository.search(searchTerm, stage, status, scopeUser, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Opportunity getOpportunity(Long id, User scopeUser) {
        Opportunity opp = opportunityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity", id));
        validateScope(opp, scopeUser);
        return opp;
    }

    @Override
    @Transactional
    public Opportunity createOpportunity(OpportunityForm form, CrmUserDetailsService.CrmUserPrincipal principal) {
        validateOpportunityRules(form.getAmount(), form.getProbability(), form.getExpectedCloseDate(), OpportunityStage.valueOf(form.getStage()), true);

        Customer customer = customerRepository.findById(form.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", form.getCustomerId()));

        Lead lead = null;
        if (form.getLeadId() != null) {
            lead = leadRepository.findById(form.getLeadId()).orElse(null);
        }

        User assignedTo = null;
        if (form.getAssignedToId() != null) {
            assignedTo = userRepository.findById(form.getAssignedToId()).orElse(null);
        }
        if (assignedTo == null && principal != null) {
            assignedTo = principal.getUser();
        }

        OpportunityStage stage = OpportunityStage.valueOf(form.getStage());

        Opportunity opportunity = Opportunity.builder()
                .name(form.getName().trim())
                .customer(customer)
                .lead(lead)
                .stage(stage)
                .amount(form.getAmount())
                .probability(form.getProbability())
                .expectedCloseDate(form.getExpectedCloseDate())
                .description(form.getDescription())
                .assignedTo(assignedTo)
                .createdBy(principal != null ? principal.getUser() : null)
                .build();

        opportunity.deriveFields();
        opportunity = opportunityRepository.save(opportunity);
        auditLogService.logCreate("Opportunity", opportunity.getId(), opportunity.getName());
        return opportunity;
    }

    @Override
    @Transactional
    public Opportunity updateOpportunity(Long id, OpportunityForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Opportunity opportunity = getOpportunity(id, scopeUser);

        OpportunityStage newStage = OpportunityStage.valueOf(form.getStage());
        validateOpportunityRules(form.getAmount(), form.getProbability(), form.getExpectedCloseDate(), newStage, false);

        String oldSnapshot = opportunity.getName() + " [Amount: " + opportunity.getAmount() + ", Stage: " + opportunity.getStage() + "]";

        Customer customer = customerRepository.findById(form.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", form.getCustomerId()));

        opportunity.setName(form.getName().trim());
        opportunity.setCustomer(customer);
        opportunity.setStage(newStage);
        opportunity.setAmount(form.getAmount());
        opportunity.setProbability(form.getProbability());
        opportunity.setExpectedCloseDate(form.getExpectedCloseDate());
        opportunity.setDescription(form.getDescription());

        if (form.getLeadId() != null) {
            leadRepository.findById(form.getLeadId()).ifPresent(opportunity::setLead);
        } else {
            opportunity.setLead(null);
        }

        if (form.getAssignedToId() != null) {
            userRepository.findById(form.getAssignedToId()).ifPresent(opportunity::setAssignedTo);
        }

        opportunity.deriveFields();
        opportunity = opportunityRepository.save(opportunity);
        auditLogService.logUpdate("Opportunity", opportunity.getId(), oldSnapshot,
                opportunity.getName() + " [Amount: " + opportunity.getAmount() + ", Stage: " + opportunity.getStage() + "]");
        return opportunity;
    }

    @Override
    @Transactional
    public void deleteOpportunity(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Opportunity opportunity = getOpportunity(id, scopeUser);
        auditLogService.logDelete("Opportunity", opportunity.getId(), "Deleted opportunity " + opportunity.getName());
        opportunityRepository.delete(opportunity);
    }

    @Override
    public OpportunityForm toForm(Opportunity opportunity) {
        OpportunityForm form = new OpportunityForm();
        form.setId(opportunity.getId());
        form.setName(opportunity.getName());
        form.setCustomerId(opportunity.getCustomer().getId());
        if (opportunity.getLead() != null) {
            form.setLeadId(opportunity.getLead().getId());
        }
        form.setStage(opportunity.getStage().name());
        form.setAmount(opportunity.getAmount());
        form.setProbability(opportunity.getProbability());
        form.setExpectedCloseDate(opportunity.getExpectedCloseDate());
        form.setDescription(opportunity.getDescription());
        if (opportunity.getAssignedTo() != null) {
            form.setAssignedToId(opportunity.getAssignedTo().getId());
        }
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Opportunity> getPipeline(User scopeUser) {
        if (scopeUser == null) {
            return opportunityRepository.findAll();
        }
        return opportunityRepository.findAll().stream()
                .filter(o -> o.getAssignedTo() != null && o.getAssignedTo().getId().equals(scopeUser.getId()))
                .toList();
    }

    private void validateOpportunityRules(BigDecimal amount, Integer probability, LocalDate closeDate, OpportunityStage stage, boolean isNew) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Opportunity Amount must be greater than 0.");
        }
        if (probability == null || probability < 0 || probability > 100) {
            throw new BusinessException("Probability must be between 0 and 100.");
        }
        // "Expected Close Date cannot be in the past." (only when stage is not WON/LOST; do not apply to old WON/LOST records on edit)
        if (stage != OpportunityStage.WON && stage != OpportunityStage.LOST) {
            if (closeDate != null && closeDate.isBefore(LocalDate.now())) {
                throw new BusinessException("Expected Close Date cannot be in the past.");
            }
        }
    }

    private void validateScope(Opportunity opportunity, User scopeUser) {
        if (scopeUser == null) return;
        boolean isAssigned = opportunity.getAssignedTo() != null && opportunity.getAssignedTo().getId().equals(scopeUser.getId());
        boolean isCreated = opportunity.getCreatedBy() != null && opportunity.getCreatedBy().getId().equals(scopeUser.getId());
        if (!isAssigned && !isCreated) {
            throw new AccessDeniedException("Access denied: You are not authorized to access opportunity ID " + opportunity.getId());
        }
    }
}
