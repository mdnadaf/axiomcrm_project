package com.acxiom.crm.service.impl;

import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.FollowUp;
import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.FollowUpStatus;
import com.acxiom.crm.enums.FollowUpType;
import com.acxiom.crm.exception.BusinessException;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.FollowUpRepository;
import com.acxiom.crm.repository.LeadRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.service.AuditLogService;
import com.acxiom.crm.service.FollowUpService;
import com.acxiom.crm.viewmodel.FollowUpForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowUpServiceImpl implements FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUp> listFollowUps(User scopeUser, FollowUpStatus status, LocalDate fromDate, LocalDate toDate, Pageable pageable) {
        return followUpRepository.search(status, scopeUser, fromDate, toDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowUp getFollowUp(Long id, User scopeUser) {
        FollowUp followUp = followUpRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FollowUp", id));
        validateScope(followUp, scopeUser);
        return followUp;
    }

    @Override
    @Transactional
    public FollowUp createFollowUp(FollowUpForm form, CrmUserDetailsService.CrmUserPrincipal principal) {
        // Business rule: Follow-up date cannot be earlier than today for a new/planned follow-up
        if (form.getFollowUpDate() == null || form.getFollowUpDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Follow-up date cannot be earlier than today.");
        }

        Customer customer = null;
        if (form.getCustomerId() != null) {
            customer = customerRepository.findById(form.getCustomerId()).orElse(null);
        }

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

        FollowUpType type = FollowUpType.valueOf(form.getType().toUpperCase());
        FollowUpStatus status = form.getStatus() != null ? FollowUpStatus.valueOf(form.getStatus().toUpperCase()) : FollowUpStatus.PLANNED;

        FollowUp followUp = FollowUp.builder()
                .followUpDate(form.getFollowUpDate())
                .type(type)
                .remarks(form.getRemarks())
                .status(status)
                .customer(customer)
                .lead(lead)
                .assignedTo(assignedTo)
                .createdBy(principal != null ? principal.getUser() : null)
                .build();

        followUp = followUpRepository.save(followUp);
        auditLogService.logCreate("FollowUp", followUp.getId(), "FollowUp: " + followUp.getType() + " on " + followUp.getFollowUpDate());
        return followUp;
    }

    @Override
    @Transactional
    public FollowUp updateFollowUp(Long id, FollowUpForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        FollowUp followUp = getFollowUp(id, scopeUser);

        if (form.getStatus() != null && "PLANNED".equalsIgnoreCase(form.getStatus())) {
            if (form.getFollowUpDate() != null && form.getFollowUpDate().isBefore(LocalDate.now())) {
                throw new BusinessException("Follow-up date cannot be earlier than today.");
            }
        }

        String oldSnapshot = followUp.getType() + " on " + followUp.getFollowUpDate() + " (" + followUp.getStatus() + ")";

        followUp.setFollowUpDate(form.getFollowUpDate());
        followUp.setType(FollowUpType.valueOf(form.getType().toUpperCase()));
        followUp.setRemarks(form.getRemarks());
        if (form.getStatus() != null) {
            followUp.setStatus(FollowUpStatus.valueOf(form.getStatus().toUpperCase()));
        }

        if (form.getCustomerId() != null) {
            customerRepository.findById(form.getCustomerId()).ifPresent(followUp::setCustomer);
        } else {
            followUp.setCustomer(null);
        }

        if (form.getLeadId() != null) {
            leadRepository.findById(form.getLeadId()).ifPresent(followUp::setLead);
        } else {
            followUp.setLead(null);
        }

        if (form.getAssignedToId() != null) {
            userRepository.findById(form.getAssignedToId()).ifPresent(followUp::setAssignedTo);
        }

        followUp = followUpRepository.save(followUp);
        auditLogService.logUpdate("FollowUp", followUp.getId(), oldSnapshot,
                followUp.getType() + " on " + followUp.getFollowUpDate() + " (" + followUp.getStatus() + ")");
        return followUp;
    }

    @Override
    @Transactional
    public FollowUp completeFollowUp(Long id, String completionNotes, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        FollowUp followUp = getFollowUp(id, scopeUser);
        followUp.setStatus(FollowUpStatus.COMPLETED);
        followUp.setCompletionNotes(completionNotes);
        followUp = followUpRepository.save(followUp);
        auditLogService.logCustom("COMPLETE_FOLLOWUP", "FollowUp", followUp.getId(), "PLANNED", "COMPLETED: " + completionNotes);
        return followUp;
    }

    @Override
    @Transactional
    public FollowUp rescheduleFollowUp(Long id, LocalDate newDate, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        if (newDate == null || newDate.isBefore(LocalDate.now())) {
            throw new BusinessException("Follow-up date cannot be earlier than today.");
        }
        FollowUp followUp = getFollowUp(id, scopeUser);
        LocalDate oldDate = followUp.getFollowUpDate();
        followUp.setRescheduledDate(newDate);
        followUp.setFollowUpDate(newDate);
        followUp.setStatus(FollowUpStatus.PLANNED);
        followUp = followUpRepository.save(followUp);
        auditLogService.logCustom("RESCHEDULE_FOLLOWUP", "FollowUp", followUp.getId(), oldDate.toString(), newDate.toString());
        return followUp;
    }

    @Override
    @Transactional
    public FollowUp cancelFollowUp(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        FollowUp followUp = getFollowUp(id, scopeUser);
        followUp.setStatus(FollowUpStatus.CANCELLED);
        followUp = followUpRepository.save(followUp);
        auditLogService.logCustom("CANCEL_FOLLOWUP", "FollowUp", followUp.getId(), "PLANNED", "CANCELLED");
        return followUp;
    }

    @Override
    @Transactional
    public void deleteFollowUp(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        FollowUp followUp = getFollowUp(id, scopeUser);
        auditLogService.logDelete("FollowUp", followUp.getId(), "Deleted follow-up on " + followUp.getFollowUpDate());
        followUpRepository.delete(followUp);
    }

    @Override
    public FollowUpForm toForm(FollowUp followUp) {
        FollowUpForm form = new FollowUpForm();
        form.setId(followUp.getId());
        form.setFollowUpDate(followUp.getFollowUpDate());
        form.setType(followUp.getType().name());
        form.setRemarks(followUp.getRemarks());
        form.setStatus(followUp.getStatus().name());
        if (followUp.getCustomer() != null) form.setCustomerId(followUp.getCustomer().getId());
        if (followUp.getLead() != null) form.setLeadId(followUp.getLead().getId());
        if (followUp.getAssignedTo() != null) form.setAssignedToId(followUp.getAssignedTo().getId());
        form.setRescheduledDate(followUp.getRescheduledDate());
        form.setCompletionNotes(followUp.getCompletionNotes());
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUp> getPendingFollowUps(User scopeUser, Pageable pageable) {
        return followUpRepository.search(FollowUpStatus.PLANNED, scopeUser, LocalDate.now(), null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowUp> getOverdueFollowUps(User scopeUser, Pageable pageable) {
        return followUpRepository.search(FollowUpStatus.MISSED, scopeUser, null, null, pageable);
    }

    @Override
    @Transactional
    public int markOverdueAsMissed() {
        List<FollowUp> overdue = followUpRepository.findOverduePlanned(LocalDate.now());
        for (FollowUp f : overdue) {
            f.setStatus(FollowUpStatus.MISSED);
            followUpRepository.save(f);
            auditLogService.logCustom("AUTO_MARK_MISSED", "FollowUp", f.getId(), "PLANNED", "MISSED");
        }
        log.info("FollowUpScheduler: marked {} follow-ups as MISSED", overdue.size());
        return overdue.size();
    }

    private void validateScope(FollowUp followUp, User scopeUser) {
        if (scopeUser == null) return;
        boolean isAssigned = followUp.getAssignedTo() != null && followUp.getAssignedTo().getId().equals(scopeUser.getId());
        boolean isCreated = followUp.getCreatedBy() != null && followUp.getCreatedBy().getId().equals(scopeUser.getId());
        if (!isAssigned && !isCreated) {
            throw new AccessDeniedException("Access denied: You are not authorized to access follow-up ID " + followUp.getId());
        }
    }
}
