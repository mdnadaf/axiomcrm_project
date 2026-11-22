package com.acxiom.crm.service.impl;

import com.acxiom.crm.entity.AuditLog;
import com.acxiom.crm.repository.AuditLogRepository;
import com.acxiom.crm.service.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.acxiom.crm.security.CrmUserDetailsService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void logCreate(String entityName, Long recordId, Object newValue) {
        saveLog("CREATE", entityName, recordId, null, serialize(newValue));
    }

    @Override
    public void logUpdate(String entityName, Long recordId, Object oldValue, Object newValue) {
        saveLog("UPDATE", entityName, recordId, serialize(oldValue), serialize(newValue));
    }

    @Override
    public void logDelete(String entityName, Long recordId, Object oldValue) {
        saveLog("DELETE", entityName, recordId, serialize(oldValue), null);
    }

    @Override
    public void logAuth(String action, String username, String details) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityName("AUTH")
                .username(username)
                .newValue(details)
                .ipAddress(getIpAddress())
                .build();
        auditLogRepository.save(auditLog);
    }

    @Override
    public void logAuth(String action, Long userId, String username, String ipAddress) {
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityName("AUTH")
                .userId(userId)
                .username(username)
                .newValue(action)
                .ipAddress(ipAddress != null ? ipAddress : getIpAddress())
                .build();
        auditLogRepository.save(auditLog);
    }

    @Override
    public void logCustom(String action, String entityName, Long recordId, Object oldValue, Object newValue) {
        saveLog(action, entityName, recordId, serialize(oldValue), serialize(newValue));
    }

    @Override
    public Page<AuditLog> search(Long userId, String entityName, String action,
                                  LocalDateTime fromDate, LocalDateTime toDate, Pageable pageable) {
        return auditLogRepository.search(userId, entityName, action, fromDate, toDate, pageable);
    }

    @Override
    public Page<AuditLog> findByEntityAndRecord(String entityName, Long recordId, Pageable pageable) {
        return auditLogRepository.findByEntityNameAndRecordId(entityName, recordId, pageable);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void saveLog(String action, String entityName, Long recordId,
                         String oldValue, String newValue) {
        Long userId = null;
        String username = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CrmUserDetailsService.CrmUserPrincipal principal) {
            userId = principal.getId();
            username = principal.getUsername();
        } else if (auth != null && auth.getPrincipal() instanceof String s) {
            username = s;
        }

        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .entityName(entityName)
                .recordId(recordId)
                .userId(userId)
                .username(username)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(getIpAddress())
                .build();

        auditLogRepository.save(auditLog);
    }

    private String serialize(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize audit value for {}: {}", obj.getClass().getSimpleName(), e.getMessage());
            return obj.toString();
        }
    }

    private String getIpAddress() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String ip = attrs.getRequest().getHeader("X-Forwarded-For");
                if (ip != null && !ip.isBlank()) return ip.split(",")[0].trim();
                return attrs.getRequest().getRemoteAddr();
            }
        } catch (Exception e) {
            log.debug("Could not determine IP address: {}", e.getMessage());
        }
        return null;
    }
}
