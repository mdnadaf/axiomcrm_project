package com.acxiom.crm.util;

import com.acxiom.crm.dto.*;
import com.acxiom.crm.entity.*;

/**
 * Utility class with static null-safe mapping methods from entities to DTOs.
 * No MapStruct — written manually so it compiles without annotation processing.
 */
public final class EntityMapper {

    private EntityMapper() {
        // utility class
    }

    // ------------------------------------------------------------------ User

    public static UserDto toDto(User user) {
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .active(user.isActive())
                .failedLoginCount(user.getFailedLoginCount())
                .lockoutEnd(user.getLockoutEnd())
                .createdDate(user.getCreatedDate())
                .build();
    }

    // --------------------------------------------------------------- Customer

    public static CustomerDto toDto(Customer customer) {
        if (customer == null) return null;
        return CustomerDto.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .name(customer.getName())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .company(customer.getCompany())
                .address(customer.getAddress())
                .notes(customer.getNotes())
                .active(customer.isActive())
                .assignedToId(customer.getAssignedTo() != null ? customer.getAssignedTo().getId() : null)
                .assignedToName(customer.getAssignedTo() != null ? customer.getAssignedTo().getFullName() : null)
                .createdById(customer.getCreatedBy() != null ? customer.getCreatedBy().getId() : null)
                .createdDate(customer.getCreatedDate())
                .updatedDate(customer.getUpdatedDate())
                .build();
    }

    // ------------------------------------------------------------------ Lead

    public static LeadDto toDto(Lead lead) {
        if (lead == null) return null;
        return LeadDto.builder()
                .id(lead.getId())
                .leadCode(lead.getLeadCode())
                .name(lead.getName())
                .company(lead.getCompany())
                .email(lead.getEmail())
                .phone(lead.getPhone())
                .status(lead.getStatus() != null ? lead.getStatus().name() : null)
                .source(lead.getSource())
                .notes(lead.getNotes())
                .expectedValue(lead.getExpectedValue())
                .expectedCloseDate(lead.getExpectedCloseDate())
                .customerId(lead.getCustomer() != null ? lead.getCustomer().getId() : null)
                .assignedToId(lead.getAssignedTo() != null ? lead.getAssignedTo().getId() : null)
                .assignedToName(lead.getAssignedTo() != null ? lead.getAssignedTo().getFullName() : null)
                .createdDate(lead.getCreatedDate())
                .build();
    }

    // ------------------------------------------------------------ Opportunity

    public static OpportunityDto toDto(Opportunity opp) {
        if (opp == null) return null;
        return OpportunityDto.builder()
                .id(opp.getId())
                .name(opp.getName())
                .customerId(opp.getCustomer() != null ? opp.getCustomer().getId() : null)
                .customerName(opp.getCustomer() != null ? opp.getCustomer().getName() : null)
                .leadId(opp.getLead() != null ? opp.getLead().getId() : null)
                .stage(opp.getStage() != null ? opp.getStage().name() : null)
                .status(opp.getStatus() != null ? opp.getStatus().name() : null)
                .amount(opp.getAmount())
                .probability(opp.getProbability())
                .weightedValue(opp.getWeightedValue())
                .expectedCloseDate(opp.getExpectedCloseDate())
                .description(opp.getDescription())
                .assignedToId(opp.getAssignedTo() != null ? opp.getAssignedTo().getId() : null)
                .assignedToName(opp.getAssignedTo() != null ? opp.getAssignedTo().getFullName() : null)
                .createdDate(opp.getCreatedDate())
                .build();
    }

    // --------------------------------------------------------------- FollowUp

    public static FollowUpDto toDto(FollowUp followUp) {
        if (followUp == null) return null;
        return FollowUpDto.builder()
                .id(followUp.getId())
                .followUpDate(followUp.getFollowUpDate())
                .type(followUp.getType() != null ? followUp.getType().name() : null)
                .remarks(followUp.getRemarks())
                .status(followUp.getStatus() != null ? followUp.getStatus().name() : null)
                .customerId(followUp.getCustomer() != null ? followUp.getCustomer().getId() : null)
                .customerName(followUp.getCustomer() != null ? followUp.getCustomer().getName() : null)
                .leadId(followUp.getLead() != null ? followUp.getLead().getId() : null)
                .assignedToId(followUp.getAssignedTo() != null ? followUp.getAssignedTo().getId() : null)
                .assignedToName(followUp.getAssignedTo() != null ? followUp.getAssignedTo().getFullName() : null)
                .rescheduledDate(followUp.getRescheduledDate())
                .completionNotes(followUp.getCompletionNotes())
                .createdDate(followUp.getCreatedDate())
                .build();
    }

    // --------------------------------------------------------------- Activity

    public static ActivityDto toDto(Activity activity) {
        if (activity == null) return null;
        return ActivityDto.builder()
                .id(activity.getId())
                .type(activity.getType() != null ? activity.getType().name() : null)
                .subject(activity.getSubject())
                .description(activity.getDescription())
                .activityDate(activity.getActivityDate())
                .status(activity.getStatus() != null ? activity.getStatus().name() : null)
                .customerId(activity.getCustomer() != null ? activity.getCustomer().getId() : null)
                .customerName(activity.getCustomer() != null ? activity.getCustomer().getName() : null)
                .leadId(activity.getLead() != null ? activity.getLead().getId() : null)
                .assignedToId(activity.getAssignedTo() != null ? activity.getAssignedTo().getId() : null)
                .assignedToName(activity.getAssignedTo() != null ? activity.getAssignedTo().getFullName() : null)
                .createdDate(activity.getCreatedDate())
                .build();
    }

    // -------------------------------------------------------------- AuditLog

    public static AuditLogDto toDto(AuditLog log) {
        if (log == null) return null;
        return AuditLogDto.builder()
                .id(log.getId())
                .userId(log.getUserId())
                .username(log.getUsername())
                .action(log.getAction())
                .entityName(log.getEntityName())
                .recordId(log.getRecordId())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .ipAddress(log.getIpAddress())
                .createdDate(log.getCreatedDate())
                .build();
    }
}
