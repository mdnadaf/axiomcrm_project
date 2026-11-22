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
public class AuditLogDto {

    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String entityName;
    private Long recordId;
    private String oldValue;
    private String newValue;
    private String ipAddress;
    private LocalDateTime createdDate;
}
