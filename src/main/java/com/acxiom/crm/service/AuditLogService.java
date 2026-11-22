package com.acxiom.crm.service;

import com.acxiom.crm.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface AuditLogService {

    void logCreate(String entityName, Long recordId, Object newValue);

    void logUpdate(String entityName, Long recordId, Object oldValue, Object newValue);

    void logDelete(String entityName, Long recordId, Object oldValue);

    void logAuth(String action, String username, String details);

    void logAuth(String action, Long userId, String username, String ipAddress);

    void logCustom(String action, String entityName, Long recordId, Object oldValue, Object newValue);

    Page<AuditLog> search(Long userId, String entityName, String action,
                          LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable);

    Page<AuditLog> findByEntityAndRecord(String entityName, Long recordId, Pageable pageable);
}
