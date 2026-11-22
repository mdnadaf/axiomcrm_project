package com.acxiom.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FollowUpDto {

    private Long id;
    private LocalDate followUpDate;
    private String type;
    private String remarks;
    private String status;
    private Long customerId;
    private String customerName;
    private Long leadId;
    private Long assignedToId;
    private String assignedToName;
    private LocalDate rescheduledDate;
    private String completionNotes;
    private LocalDateTime createdDate;
}
