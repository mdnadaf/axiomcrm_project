package com.acxiom.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityDto {

    private Long id;
    private String type;
    private String subject;
    private String description;
    private LocalDateTime activityDate;
    private String status;
    private Long customerId;
    private String customerName;
    private Long leadId;
    private Long assignedToId;
    private String assignedToName;
    private LocalDateTime createdDate;
}
