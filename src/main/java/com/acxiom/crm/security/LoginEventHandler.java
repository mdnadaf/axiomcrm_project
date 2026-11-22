package com.acxiom.crm.security;

import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.Role;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.service.AuditLogService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoginEventHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Value("${app.security.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.lockout-duration-minutes:15}")
    private int lockoutDurationMinutes;

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication auth) throws IOException, ServletException {
        String email = auth.getName();
        userRepository.findByEmail(email).ifPresent(user -> {
            user.setFailedLoginCount(0);
            user.setLockoutEnd(null);
            userRepository.save(user);
            auditLogService.logAuth("LOGIN_SUCCESS", user.getId(), user.getEmail(), request.getRemoteAddr());
        });
        response.sendRedirect(request.getContextPath() + "/dashboard");
    }

    @Override
    @Transactional
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException ex) throws IOException, ServletException {
        String email = request.getParameter("username");
        if (email != null && !email.isBlank()) {
            userRepository.findByEmail(email).ifPresent(user -> {
                int failedCount = user.getFailedLoginCount() + 1;
                user.setFailedLoginCount(failedCount);
                auditLogService.logAuth("LOGIN_FAILED", user.getId(), user.getEmail(), request.getRemoteAddr());

                if (failedCount >= maxFailedAttempts && user.getLockoutEnd() == null) {
                    user.setLockoutEnd(LocalDateTime.now().plusMinutes(lockoutDurationMinutes));
                    auditLogService.logAuth("ACCOUNT_LOCKED", user.getId(), user.getEmail(), request.getRemoteAddr());
                    log.warn("Account locked for user: {}", email);
                }
                userRepository.save(user);
            });
        }
        response.sendRedirect(request.getContextPath() + "/login?error");
    }
}
