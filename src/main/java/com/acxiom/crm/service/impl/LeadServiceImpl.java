package com.acxiom.crm.service.impl;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.Opportunity;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.LeadStatus;
import com.acxiom.crm.enums.OpportunityStage;
import com.acxiom.crm.exception.InvalidLeadStatusTransitionException;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.LeadRepository;
import com.acxiom.crm.repository.OpportunityRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.service.AuditLogService;
import com.acxiom.crm.service.LeadService;
import com.acxiom.crm.viewmodel.LeadForm;
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
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final OpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<Lead> listLeads(User scopeUser, String search, String status, Pageable pageable) {
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        LeadStatus leadStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                leadStatus = LeadStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return leadRepository.search(searchTerm, leadStatus, scopeUser, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Lead getLead(Long id, User scopeUser) {
        Lead lead = leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead", id));
        validateScope(lead, scopeUser);
        return lead;
    }

    @Override
    @Transactional
    public Lead createLead(LeadForm form, CrmUserDetailsService.CrmUserPrincipal principal) {
        User assignedTo = null;
        if (form.getAssignedToId() != null) {
            assignedTo = userRepository.findById(form.getAssignedToId()).orElse(null);
        }
        if (assignedTo == null && principal != null) {
            assignedTo = principal.getUser();
        }

        User createdBy = principal != null ? principal.getUser() : null;

        LeadStatus status = LeadStatus.NEW;
        if (form.getStatus() != null && !form.getStatus().isBlank()) {
            status = LeadStatus.valueOf(form.getStatus().toUpperCase());
        }

        Customer customer = null;
        if (form.getCustomerId() != null) {
            customer = customerRepository.findById(form.getCustomerId()).orElse(null);
        }

        int nextSeq = leadRepository.findMaxLeadCodeSequence() + 1;
        String leadCode = String.format("LEAD-%04d", nextSeq);

        Lead lead = Lead.builder()
                .leadCode(leadCode)
                .name(form.getName().trim())
                .company(form.getCompany())
                .email(form.getEmail().trim().toLowerCase())
                .phone(form.getPhone() != null ? form.getPhone().trim() : null)
                .status(status)
                .source(form.getSource())
                .notes(form.getNotes())
                .expectedValue(form.getExpectedValue() != null ? form.getExpectedValue() : BigDecimal.ZERO)
                .expectedCloseDate(form.getExpectedCloseDate())
                .customer(customer)
                .assignedTo(assignedTo)
                .createdBy(createdBy)
                .build();

        lead = leadRepository.save(lead);
        auditLogService.logCreate("Lead", lead.getId(), lead.getName());
        return lead;
    }

    @Override
    @Transactional
    public Lead updateLead(Long id, LeadForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Lead lead = getLead(id, scopeUser);

        String oldSnapshot = lead.getName() + " (" + lead.getStatus() + ")";

        LeadStatus targetStatus = LeadStatus.valueOf(form.getStatus().toUpperCase());
        if (lead.getStatus() != targetStatus) {
            validateStatusTransition(lead.getStatus(), targetStatus);
            lead.setStatus(targetStatus);
        }

        lead.setName(form.getName().trim());
        lead.setCompany(form.getCompany());
        lead.setEmail(form.getEmail().trim().toLowerCase());
        lead.setPhone(form.getPhone() != null ? form.getPhone().trim() : null);
        lead.setSource(form.getSource());
        lead.setNotes(form.getNotes());
        lead.setExpectedValue(form.getExpectedValue());
        lead.setExpectedCloseDate(form.getExpectedCloseDate());

        if (form.getCustomerId() != null) {
            customerRepository.findById(form.getCustomerId()).ifPresent(lead::setCustomer);
        }

        if (form.getAssignedToId() != null) {
            userRepository.findById(form.getAssignedToId()).ifPresent(lead::setAssignedTo);
        }

        lead = leadRepository.save(lead);
        auditLogService.logUpdate("Lead", lead.getId(), oldSnapshot, lead.getName() + " (" + lead.getStatus() + ")");
        return lead;
    }

    @Override
    @Transactional
    public void deleteLead(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Lead lead = getLead(id, scopeUser);
        auditLogService.logDelete("Lead", lead.getId(), "Deleted lead " + lead.getName());
        leadRepository.delete(lead);
    }

    @Override
    @Transactional
    public Customer convertLead(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Lead lead = getLead(id, scopeUser);

        if (lead.getStatus() != LeadStatus.QUALIFIED) {
            throw new IllegalStateException("Only QUALIFIED leads can be converted. Current status: " + lead.getStatus());
        }

        // Reuse or create Customer
        Customer customer = customerRepository.findByEmail(lead.getEmail()).orElse(null);
        if (customer == null) {
            int nextCustSeq = customerRepository.findMaxCustomerCodeSequence() + 1;
            String customerCode = String.format("CUST-%04d", nextCustSeq);

            customer = Customer.builder()
                    .customerCode(customerCode)
                    .name(lead.getName())
                    .email(lead.getEmail())
                    .phone(lead.getPhone() != null && !lead.getPhone().isBlank() ? lead.getPhone() : "9999999999")
                    .company(lead.getCompany())
                    .notes("Converted from Lead " + lead.getLeadCode())
                    .active(true)
                    .assignedTo(lead.getAssignedTo())
                    .createdBy(principal != null ? principal.getUser() : lead.getCreatedBy())
                    .build();
            customer = customerRepository.save(customer);
            auditLogService.logCreate("Customer", customer.getId(), customer.getName());
        }

        // Create Opportunity
        BigDecimal amount = (lead.getExpectedValue() != null && lead.getExpectedValue().compareTo(BigDecimal.ZERO) > 0)
                ? lead.getExpectedValue() : BigDecimal.valueOf(1000.00);

        LocalDate closeDate = (lead.getExpectedCloseDate() != null && !lead.getExpectedCloseDate().isBefore(LocalDate.now()))
                ? lead.getExpectedCloseDate() : LocalDate.now().plusMonths(1);

        Opportunity opportunity = Opportunity.builder()
                .name(lead.getName() + " Opportunity")
                .customer(customer)
                .lead(lead)
                .stage(OpportunityStage.QUALIFICATION)
                .amount(amount)
                .probability(20)
                .expectedCloseDate(closeDate)
                .description("Generated from Lead conversion (" + lead.getLeadCode() + ")")
                .assignedTo(lead.getAssignedTo())
                .createdBy(principal != null ? principal.getUser() : lead.getCreatedBy())
                .build();
        opportunity.deriveFields();
        opportunityRepository.save(opportunity);
        auditLogService.logCreate("Opportunity", opportunity.getId(), opportunity.getName());

        // Update Lead status
        lead.setStatus(LeadStatus.CONVERTED);
        lead.setCustomer(customer);
        leadRepository.save(lead);
        auditLogService.logCustom("CONVERT_LEAD", "Lead", lead.getId(), LeadStatus.QUALIFIED.name(), LeadStatus.CONVERTED.name());

        return customer;
    }

    @Override
    @Transactional
    public Lead changeStatus(Long id, String newStatus, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Lead lead = getLead(id, scopeUser);
        LeadStatus targetStatus = LeadStatus.valueOf(newStatus.toUpperCase());
        validateStatusTransition(lead.getStatus(), targetStatus);

        LeadStatus oldStatus = lead.getStatus();
        lead.setStatus(targetStatus);
        lead = leadRepository.save(lead);
        auditLogService.logUpdate("Lead", lead.getId(), oldStatus.name(), targetStatus.name());
        return lead;
    }

    @Override
    public LeadForm toForm(Lead lead) {
        LeadForm form = new LeadForm();
        form.setId(lead.getId());
        form.setName(lead.getName());
        form.setCompany(lead.getCompany());
        form.setEmail(lead.getEmail());
        form.setPhone(lead.getPhone());
        form.setStatus(lead.getStatus().name());
        form.setSource(lead.getSource());
        form.setNotes(lead.getNotes());
        form.setExpectedValue(lead.getExpectedValue());
        form.setExpectedCloseDate(lead.getExpectedCloseDate());
        if (lead.getCustomer() != null) {
            form.setCustomerId(lead.getCustomer().getId());
        }
        if (lead.getAssignedTo() != null) {
            form.setAssignedToId(lead.getAssignedTo().getId());
        }
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lead> listAllActive(User scopeUser) {
        if (scopeUser == null) {
            return leadRepository.findAll().stream()
                    .filter(l -> l.getStatus() != LeadStatus.CONVERTED && l.getStatus() != LeadStatus.LOST)
                    .toList();
        }
        return leadRepository.findAll().stream()
                .filter(l -> l.getStatus() != LeadStatus.CONVERTED && l.getStatus() != LeadStatus.LOST
                        && l.getAssignedTo() != null && l.getAssignedTo().getId().equals(scopeUser.getId()))
                .toList();
    }

    private void validateScope(Lead lead, User scopeUser) {
        if (scopeUser == null) return;
        boolean isAssigned = lead.getAssignedTo() != null && lead.getAssignedTo().getId().equals(scopeUser.getId());
        boolean isCreated = lead.getCreatedBy() != null && lead.getCreatedBy().getId().equals(scopeUser.getId());
        if (!isAssigned && !isCreated) {
            throw new AccessDeniedException("Access denied: You are not authorized to access lead ID " + lead.getId());
        }
    }

    /**
     * Enforce status transitions:
     * NEW -> CONTACTED, UNQUALIFIED, LOST
     * CONTACTED -> QUALIFIED, UNQUALIFIED, LOST
     * QUALIFIED -> CONVERTED, LOST
     * UNQUALIFIED -> final
     * CONVERTED -> final
     * LOST -> final
     */
    private void validateStatusTransition(LeadStatus from, LeadStatus to) {
        if (from == to) return;
        if (from == LeadStatus.CONVERTED || from == LeadStatus.LOST) {
            throw new InvalidLeadStatusTransitionException(from, to);
        }
        boolean valid = switch (from) {
            case NEW -> (to == LeadStatus.CONTACTED || to == LeadStatus.UNQUALIFIED || to == LeadStatus.LOST);
            case CONTACTED -> (to == LeadStatus.QUALIFIED || to == LeadStatus.UNQUALIFIED || to == LeadStatus.LOST);
            case QUALIFIED -> (to == LeadStatus.CONVERTED || to == LeadStatus.LOST);
            case UNQUALIFIED -> (to == LeadStatus.CONTACTED || to == LeadStatus.LOST);
            default -> false;
        };
        if (!valid) {
            throw new InvalidLeadStatusTransitionException(from, to);
        }
    }
}
