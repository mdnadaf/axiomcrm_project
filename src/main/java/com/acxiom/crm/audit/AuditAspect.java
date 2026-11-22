package com.acxiom.crm.audit;

import com.acxiom.crm.entity.AuditLog;
import com.acxiom.crm.repository.AuditLogRepository;
import com.acxiom.crm.security.CrmUserDetailsService.CrmUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    private static final ThreadLocal<String> oldValueHolder = new ThreadLocal<>();

    @Pointcut("@annotation(com.acxiom.crm.audit.Audited) && within(com.acxiom.crm.service..*)")
    public void auditedMethod() {
    }

    @Before("auditedMethod()")
    public void captureOldValue(JoinPoint jp) {
        try {
            Object[] args = jp.getArgs();
            if (args != null && args.length > 0) {
                oldValueHolder.set(toJson(args.length == 1 ? args[0] : args));
            } else {
                oldValueHolder.set(null);
            }
        } catch (Exception ex) {
            log.warn("AuditAspect: failed to capture old value - {}", ex.getMessage());
            oldValueHolder.set(null);
        }
    }

    @AfterReturning(pointcut = "auditedMethod()", returning = "returnValue")
    public void logAudit(JoinPoint jp, Object returnValue) {
        try {
            MethodSignature sig = (MethodSignature) jp.getSignature();
            Method method = sig.getMethod();
            Audited audited = method.getAnnotation(Audited.class);

            String action = audited.action();
            String entityName = audited.entity();

            Long userId = null;
            String username = "anonymous";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CrmUserPrincipal principal) {
                userId = principal.getUser().getId();
                username = principal.getUsername();
            }

            String ipAddress = resolveClientIp();
            Long recordId = extractId(returnValue);

            String oldValue = oldValueHolder.get();
            String newValue = toJson(returnValue);

            AuditLog entry = AuditLog.builder()
                    .userId(userId)
                    .username(username)
                    .action(action)
                    .entityName(entityName)
                    .recordId(recordId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .ipAddress(ipAddress)
                    .createdDate(LocalDateTime.now())
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.error("AuditAspect: failed to persist audit log - {}", ex.getMessage(), ex);
        } finally {
            oldValueHolder.remove();
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception ex) {
            log.warn("AuditAspect: cannot serialize {} - {}", obj.getClass().getSimpleName(), ex.getMessage());
            return "\"<serialization error>\"";
        }
    }

    private String resolveClientIp() {
        try {
            RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                HttpServletRequest req = sra.getRequest();
                String ip = req.getHeader("X-Forwarded-For");
                if (ip != null && !ip.isBlank()) {
                    return ip.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ex) {
            log.warn("AuditAspect: cannot resolve client IP - {}", ex.getMessage());
        }
        return "unknown";
    }

    private Long extractId(Object returnValue) {
        if (returnValue == null) return null;
        try {
            Method getId = returnValue.getClass().getMethod("getId");
            Object id = getId.invoke(returnValue);
            if (id instanceof Number n) {
                return n.longValue();
            }
            if (id != null) {
                return Long.parseLong(id.toString());
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
