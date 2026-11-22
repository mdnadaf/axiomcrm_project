package com.acxiom.crm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpportunityDto {

    private Long id;
    private String name;
    private Long customerId;
    private String customerName;
    private Long leadId;
    private String stage;
    private String status;
    private BigDecimal amount;
    private int probability;
    private BigDecimal weightedValue;
    private LocalDate expectedCloseDate;
    private String description;
    private Long assignedToId;
    private String assignedToName;
    private LocalDateTime createdDate;
}
