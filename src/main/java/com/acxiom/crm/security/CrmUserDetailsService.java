package com.acxiom.crm.security;

import com.acxiom.crm.entity.User;
import com.acxiom.crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrmUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));
        return new CrmUserPrincipal(user);
    }

    public static class CrmUserPrincipal implements UserDetails {
        private final User user;

        public CrmUserPrincipal(User user) {
            this.user = user;
        }

        public User getUser() { return user; }
        public Long getId() { return user.getId(); }
        public String getFullName() { return user.getFullName(); }
        public String getRole() { return user.getRole().name(); }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return List.of(new SimpleGrantedAuthority(user.getRole().name()));
        }

        @Override public String getPassword() { return user.getPasswordHash(); }
        @Override public String getUsername() { return user.getEmail(); }

        @Override
        public boolean isAccountNonExpired() { return true; }

        @Override
        public boolean isAccountNonLocked() {
            if (!user.isActive()) return false;
            if (user.getLockoutEnd() == null) return true;
            return LocalDateTime.now().isAfter(user.getLockoutEnd());
        }

        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public boolean isEnabled() { return user.isActive(); }
    }
}
