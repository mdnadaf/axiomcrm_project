package com.acxiom.crm.security;

import com.acxiom.crm.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogoutAuditHandler implements LogoutHandler {

    private final AuditLogService auditLogService;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            Long userId = null;
            if (authentication.getPrincipal() instanceof CrmUserDetailsService.CrmUserPrincipal principal) {
                userId = principal.getId();
            }
            auditLogService.logAuth("LOGOUT", userId, username, request.getRemoteAddr());
            log.info("User {} logged out successfully.", username);
        }
    }
}
