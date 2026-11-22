package com.acxiom.crm.config;

import com.acxiom.crm.entity.User;
import com.acxiom.crm.security.CrmUserDetailsService;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of("SYSTEM");
        }
        if (auth.getPrincipal() instanceof CrmUserDetailsService.CrmUserPrincipal principal) {
            return Optional.of(principal.getUsername());
        }
        return Optional.of(auth.getName());
    }
}
