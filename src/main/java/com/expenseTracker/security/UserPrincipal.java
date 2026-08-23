package com.expenseTracker.security;

import com.expenseTracker.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapts our User entity to Spring Security's UserDetails contract. Deliberately
 * NOT implemented directly on the entity - keeping the persistence model
 * decoupled from a Spring Security interface avoids coupling JPA's lazy-loading
 * semantics to security-context serialization, and means User.java has no
 * framework dependency beyond JPA itself.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final Collection<? extends GrantedAuthority> authorities;

    private UserPrincipal(Long id, String email, String passwordHash,
                          Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.authorities = authorities;
    }

    public static UserPrincipal fromUser(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(user.getRole().name())));
    }

    /** Convenience accessor so downstream code (controllers, JwtUtil callers)
     *  can get the numeric ID off the principal without a second DB lookup. */
    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // No account-lockout/expiry features exist yet (Sprint 2 adds brute-force
    // mitigation). Hardcoding true here is correct for today's scope, but
    // isAccountNonLocked in particular will need to become a real check once
    // that lands - it should not stay hardcoded true forever.
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}