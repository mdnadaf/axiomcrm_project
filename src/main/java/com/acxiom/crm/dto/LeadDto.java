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
public class LeadDto {

    private Long id;
    private String leadCode;
    private String name;
    private String company;
    private String email;
    private String phone;
    private String status;
    private String source;
    private String notes;
    private BigDecimal expectedValue;
    private LocalDate expectedCloseDate;
    private Long customerId;
    private Long assignedToId;
    private String assignedToName;
    private LocalDateTime createdDate;
}
