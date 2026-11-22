package com.acxiom.crm.security;

import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.Role;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService.CrmUserPrincipal;
import com.acxiom.crm.service.AuditLogService;
import com.acxiom.crm.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LockoutTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private LoginEventHandler loginEventHandler;

    @InjectMocks
    private UserServiceImpl userService;

    private User targetUser;
    private CrmUserPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(loginEventHandler, "maxFailedAttempts", 5);
        ReflectionTestUtils.setField(loginEventHandler, "lockoutDurationMinutes", 15);

        targetUser = User.builder()
                .id(10L)
                .email("sales1@acxiomcrm.com")
                .fullName("Sales User")
                .role(Role.ROLE_SALES_EXECUTIVE)
                .active(true)
                .failedLoginCount(0)
                .lockoutEnd(null)
                .build();

        User adminUser = User.builder().id(1L).email("admin@acxiomcrm.com").role(Role.ROLE_ADMIN).build();
        adminPrincipal = new CrmUserPrincipal(adminUser);
    }

    @Test
    void testFailedLoginIncrementsCount() throws Exception {
        when(request.getParameter("username")).thenReturn("sales1@acxiomcrm.com");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(userRepository.findByEmail("sales1@acxiomcrm.com")).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        loginEventHandler.onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

        assertEquals(1, targetUser.getFailedLoginCount());
        assertNull(targetUser.getLockoutEnd());
        verify(auditLogService).logAuth("LOGIN_FAILED", 10L, "sales1@acxiomcrm.com", "127.0.0.1");
    }

    @Test
    void testFifthFailedLoginTriggersLockout() throws Exception {
        targetUser.setFailedLoginCount(4);
        when(request.getParameter("username")).thenReturn("sales1@acxiomcrm.com");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(userRepository.findByEmail("sales1@acxiomcrm.com")).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        loginEventHandler.onAuthenticationFailure(request, response, new BadCredentialsException("Bad credentials"));

        assertEquals(5, targetUser.getFailedLoginCount());
        assertNotNull(targetUser.getLockoutEnd());
        assertTrue(targetUser.getLockoutEnd().isAfter(LocalDateTime.now().plusMinutes(14)));

        verify(auditLogService).logAuth("ACCOUNT_LOCKED", 10L, "sales1@acxiomcrm.com", "127.0.0.1");
    }

    @Test
    void testSuccessfulLoginResetsFailedCountAndLockout() throws Exception {
        targetUser.setFailedLoginCount(3);
        targetUser.setLockoutEnd(LocalDateTime.now().minusMinutes(5));

        when(authentication.getName()).thenReturn("sales1@acxiomcrm.com");
        when(request.getContextPath()).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(userRepository.findByEmail("sales1@acxiomcrm.com")).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        loginEventHandler.onAuthenticationSuccess(request, response, authentication);

        assertEquals(0, targetUser.getFailedLoginCount());
        assertNull(targetUser.getLockoutEnd());
        verify(auditLogService).logAuth("LOGIN_SUCCESS", 10L, "sales1@acxiomcrm.com", "127.0.0.1");
    }

    @Test
    void testAdminUnlockResetsFailedCountAndLockout() {
        targetUser.setFailedLoginCount(5);
        targetUser.setLockoutEnd(LocalDateTime.now().plusMinutes(15));

        when(userRepository.findById(10L)).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.unlockAccount(10L, adminPrincipal);

        assertEquals(0, targetUser.getFailedLoginCount());
        assertNull(targetUser.getLockoutEnd());

        verify(auditLogService).logCustom(
                eq("ACCOUNT_UNLOCKED"), eq("User"), eq(10L), eq("LOCKED"), eq("UNLOCKED by admin")
        );
    }

    @Test
    void testAccountNonLockedStatus() {
        CrmUserPrincipal activePrincipal = new CrmUserPrincipal(targetUser);
        assertTrue(activePrincipal.isAccountNonLocked());

        targetUser.setLockoutEnd(LocalDateTime.now().plusMinutes(10));
        CrmUserPrincipal lockedPrincipal = new CrmUserPrincipal(targetUser);
        assertFalse(lockedPrincipal.isAccountNonLocked());

        targetUser.setLockoutEnd(LocalDateTime.now().minusMinutes(1));
        CrmUserPrincipal expiredLockoutPrincipal = new CrmUserPrincipal(targetUser);
        assertTrue(expiredLockoutPrincipal.isAccountNonLocked());
    }
}
