package com.acxiom.crm.service.impl;

import com.acxiom.crm.entity.Activity;
import com.acxiom.crm.entity.Customer;
import com.acxiom.crm.entity.Lead;
import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.ActivityStatus;
import com.acxiom.crm.enums.ActivityType;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.repository.ActivityRepository;
import com.acxiom.crm.repository.CustomerRepository;
import com.acxiom.crm.repository.LeadRepository;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.service.ActivityService;
import com.acxiom.crm.service.AuditLogService;
import com.acxiom.crm.viewmodel.ActivityForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceImpl implements ActivityService {

    private final ActivityRepository activityRepository;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public Page<Activity> listActivities(User scopeUser, ActivityType type, ActivityStatus status,
                                          LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        return activityRepository.search(type, status, scopeUser, fromDate, toDate, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Activity getActivity(Long id, User scopeUser) {
        Activity activity = activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", id));
        validateScope(activity, scopeUser);
        return activity;
    }

    @Override
    @Transactional
    public Activity createActivity(ActivityForm form, CrmUserDetailsService.CrmUserPrincipal principal) {
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

        ActivityType type = ActivityType.valueOf(form.getType().toUpperCase());
        ActivityStatus status = form.getStatus() != null ? ActivityStatus.valueOf(form.getStatus().toUpperCase()) : ActivityStatus.PENDING;

        Activity activity = Activity.builder()
                .type(type)
                .subject(form.getSubject().trim())
                .description(form.getDescription())
                .activityDate(form.getActivityDate())
                .status(status)
                .customer(customer)
                .lead(lead)
                .assignedTo(assignedTo)
                .createdBy(principal != null ? principal.getUser() : null)
                .build();

        activity = activityRepository.save(activity);
        auditLogService.logCreate("Activity", activity.getId(), activity.getSubject());
        return activity;
    }

    @Override
    @Transactional
    public Activity updateActivity(Long id, ActivityForm form, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Activity activity = getActivity(id, scopeUser);

        String oldSnapshot = activity.getSubject() + " (" + activity.getStatus() + ")";

        activity.setType(ActivityType.valueOf(form.getType().toUpperCase()));
        activity.setSubject(form.getSubject().trim());
        activity.setDescription(form.getDescription());
        activity.setActivityDate(form.getActivityDate());
        if (form.getStatus() != null) {
            activity.setStatus(ActivityStatus.valueOf(form.getStatus().toUpperCase()));
        }

        if (form.getCustomerId() != null) {
            customerRepository.findById(form.getCustomerId()).ifPresent(activity::setCustomer);
        } else {
            activity.setCustomer(null);
        }

        if (form.getLeadId() != null) {
            leadRepository.findById(form.getLeadId()).ifPresent(activity::setLead);
        } else {
            activity.setLead(null);
        }

        if (form.getAssignedToId() != null) {
            userRepository.findById(form.getAssignedToId()).ifPresent(activity::setAssignedTo);
        }

        activity = activityRepository.save(activity);
        auditLogService.logUpdate("Activity", activity.getId(), oldSnapshot, activity.getSubject() + " (" + activity.getStatus() + ")");
        return activity;
    }

    @Override
    @Transactional
    public void deleteActivity(Long id, User scopeUser, CrmUserDetailsService.CrmUserPrincipal principal) {
        Activity activity = getActivity(id, scopeUser);
        auditLogService.logDelete("Activity", activity.getId(), "Deleted activity " + activity.getSubject());
        activityRepository.delete(activity);
    }

    @Override
    public ActivityForm toForm(Activity activity) {
        ActivityForm form = new ActivityForm();
        form.setId(activity.getId());
        form.setType(activity.getType().name());
        form.setSubject(activity.getSubject());
        form.setDescription(activity.getDescription());
        form.setActivityDate(activity.getActivityDate());
        form.setStatus(activity.getStatus().name());
        if (activity.getCustomer() != null) form.setCustomerId(activity.getCustomer().getId());
        if (activity.getLead() != null) form.setLeadId(activity.getLead().getId());
        if (activity.getAssignedTo() != null) form.setAssignedToId(activity.getAssignedTo().getId());
        return form;
    }

    private void validateScope(Activity activity, User scopeUser) {
        if (scopeUser == null) return;
        boolean isAssigned = activity.getAssignedTo() != null && activity.getAssignedTo().getId().equals(scopeUser.getId());
        boolean isCreated = activity.getCreatedBy() != null && activity.getCreatedBy().getId().equals(scopeUser.getId());
        if (!isAssigned && !isCreated) {
            throw new AccessDeniedException("Access denied: You are not authorized to access activity ID " + activity.getId());
        }
    }
}
