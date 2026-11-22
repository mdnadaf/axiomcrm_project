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
public class CustomerDto {

    private Long id;
    private String customerCode;
    private String name;
    private String email;
    private String phone;
    private String company;
    private String address;
    private String notes;
    private boolean active;
    private Long assignedToId;
    private String assignedToName;
    private Long createdById;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
