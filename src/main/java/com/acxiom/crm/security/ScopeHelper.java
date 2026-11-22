package com.acxiom.crm.security;

import com.acxiom.crm.enums.Role;
import com.acxiom.crm.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ScopeHelper {

    /**
     * Returns the User to filter by for a given principal.
     * Admin & Manager => null (all records); Sales Executive => their own User entity.
     */
    public User getScopeUser(CrmUserDetailsService.CrmUserPrincipal principal) {
        if (principal == null) return null;
        String role = principal.getRole();
        if (Role.ROLE_ADMIN.name().equals(role) || Role.ROLE_MANAGER.name().equals(role)) {
            return null; // see all
        }
        return principal.getUser(); // scope to self
    }

    public boolean isAdmin(CrmUserDetailsService.CrmUserPrincipal principal) {
        return principal != null && Role.ROLE_ADMIN.name().equals(principal.getRole());
    }

    public boolean isManager(CrmUserDetailsService.CrmUserPrincipal principal) {
        return principal != null && Role.ROLE_MANAGER.name().equals(principal.getRole());
    }

    public boolean isSalesExecutive(CrmUserDetailsService.CrmUserPrincipal principal) {
        return principal != null && Role.ROLE_SALES_EXECUTIVE.name().equals(principal.getRole());
    }

    public boolean canAdminOrManager(CrmUserDetailsService.CrmUserPrincipal principal) {
        return isAdmin(principal) || isManager(principal);
    }
}
