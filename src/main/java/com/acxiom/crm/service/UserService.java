package com.acxiom.crm.service;

import com.acxiom.crm.entity.User;
import com.acxiom.crm.enums.Role;
import com.acxiom.crm.security.CrmUserDetailsService;
import com.acxiom.crm.viewmodel.RegisterForm;
import com.acxiom.crm.viewmodel.UserForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    User register(RegisterForm form);

    User createUser(UserForm form, CrmUserDetailsService.CrmUserPrincipal principal);

    User updateUser(Long id, UserForm form, CrmUserDetailsService.CrmUserPrincipal principal);

    User getUser(Long id);

    User getByEmail(String email);

    Page<User> listUsers(String search, Role role, Boolean active, Pageable pageable);

    void toggleActive(Long id, CrmUserDetailsService.CrmUserPrincipal principal);

    void unlockAccount(Long id, CrmUserDetailsService.CrmUserPrincipal principal);

    void resetPassword(Long id, String newPassword, CrmUserDetailsService.CrmUserPrincipal principal);

    UserForm toForm(User user);

    List<User> listSalesExecutives();
}
