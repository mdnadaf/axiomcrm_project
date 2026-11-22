package com.acxiom.crm.service.impl;

import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.Role;
import com.acxiom.crm.exception.BusinessException;
import com.acxiom.crm.exception.DuplicateResourceException;
import com.acxiom.crm.exception.ResourceNotFoundException;
import com.acxiom.crm.repository.UserRepository;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.service.AuditLogService;
import com.acxiom.crm.service.UserService;
import com.acxiom.crm.viewmodel.RegisterForm;
import com.acxiom.crm.viewmodel.UserForm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Override
    @Transactional
    public User register(RegisterForm form) {
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new IllegalArgumentException("Email already in use: " + form.getEmail());
        }

        User user = User.builder()
                .fullName(form.getFullName().trim())
                .email(form.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(form.getPassword()))
                .role(Role.ROLE_SALES_EXECUTIVE) // Section 17: Registration always assigns SALES_EXECUTIVE
                .active(true)
                .failedLoginCount(0)
                .build();

        user = userRepository.save(user);
        auditLogService.logCustom("USER_REGISTER", "User", user.getId(), null, user.getEmail());
        return user;
    }

    @Override
    @Transactional
    public User createUser(UserForm form, CrmUserDetailsService.CrmUserPrincipal principal) {
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new DuplicateResourceException("User with email '" + form.getEmail() + "' already exists.");
        }
        if (form.getPassword() == null || form.getPassword().isBlank()) {
            throw new BusinessException("Password is required for new user creation.");
        }

        Role role = Role.valueOf(form.getRole());

        User user = User.builder()
                .fullName(form.getFullName().trim())
                .email(form.getEmail().trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(form.getPassword()))
                .role(role)
                .active(form.isActive())
                .failedLoginCount(0)
                .build();

        user = userRepository.save(user);
        auditLogService.logCreate("User", user.getId(), user.getEmail() + " (" + user.getRole() + ")");
        return user;
    }

    @Override
    @Transactional
    public User updateUser(Long id, UserForm form, CrmUserDetailsService.CrmUserPrincipal principal) {
        User user = getUser(id);

        if (!user.getEmail().equalsIgnoreCase(form.getEmail()) && userRepository.existsByEmail(form.getEmail())) {
            throw new DuplicateResourceException("User with email '" + form.getEmail() + "' already exists.");
        }

        String oldSnapshot = user.getEmail() + " [Role: " + user.getRole() + ", Active: " + user.isActive() + "]";

        user.setFullName(form.getFullName().trim());
        user.setEmail(form.getEmail().trim().toLowerCase());
        user.setRole(Role.valueOf(form.getRole()));
        user.setActive(form.isActive());

        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(form.getPassword()));
            auditLogService.logCustom("ADMIN_PASSWORD_RESET", "User", user.getId(), null, "Password reset by admin");
        }

        user = userRepository.save(user);
        auditLogService.logUpdate("User", user.getId(), oldSnapshot,
                user.getEmail() + " [Role: " + user.getRole() + ", Active: " + user.isActive() + "]");
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User with email " + email, 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> listUsers(String search, Role role, Boolean active, Pageable pageable) {
        String searchTerm = (search != null && !search.isBlank()) ? search.trim() : null;
        return userRepository.search(searchTerm, role, active, pageable);
    }

    @Override
    @Transactional
    public void toggleActive(Long id, CrmUserDetailsService.CrmUserPrincipal principal) {
        User user = getUser(id);
        boolean newActive = !user.isActive();
        user.setActive(newActive);
        userRepository.save(user);
        auditLogService.logCustom("USER_STATUS_CHANGE", "User", user.getId(),
                "Active: " + !newActive, "Active: " + newActive);
    }

    @Override
    @Transactional
    public void unlockAccount(Long id, CrmUserDetailsService.CrmUserPrincipal principal) {
        User user = getUser(id);
        user.setFailedLoginCount(0);
        user.setLockoutEnd(null);
        userRepository.save(user);
        auditLogService.logCustom("ACCOUNT_UNLOCKED", "User", user.getId(), "LOCKED", "UNLOCKED by admin");
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String newPassword, CrmUserDetailsService.CrmUserPrincipal principal) {
        User user = getUser(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditLogService.logCustom("ADMIN_PASSWORD_RESET", "User", user.getId(), null, "Password reset by admin");
    }

    @Override
    public UserForm toForm(User user) {
        UserForm form = new UserForm();
        form.setId(user.getId());
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setRole(user.getRole().name());
        form.setActive(user.isActive());
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> listSalesExecutives() {
        return userRepository.findByRole(Role.ROLE_SALES_EXECUTIVE);
    }
}
