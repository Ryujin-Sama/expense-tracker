package com.expenseTracker.security;

import com.expenseTracker.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * User Principal - Spring Security's view of the authenticated user.
 * 
 * This class adapts the User entity (JPA domain model) to Spring Security's
 * UserDetails interface. It serves as the "security principal" - the
 * representation of a user within the Spring Security context.
 * 
 * Design Pattern: Adapter Pattern
 * - User is the domain entity (pure persistence model)
 * - UserPrincipal is the security context adapter
 * - They are separate classes (not User implements UserDetails)
 * 
 * Why Separate Classes?
 * Keeping the domain model decoupled from framework interfaces has benefits:
 * 
 * 1. Low Coupling:
 *    - User entity has no Spring Security dependencies
 *    - User.java only depends on JPA (jakarta.persistence.*)
 *    - Easy to use User in non-security contexts
 *    - User is portable (could use with different security framework)
 * 
 * 2. Serialization:
 *    - UserDetails implementations have complex serialization requirements
 *    - JPA entities have lazy-loading, proxy objects, etc.
 *    - Spring Security sessions need to serialize principals
 *    - Mixing both concerns in User would cause issues
 * 
 * 3. Separation of Concerns:
 *    - User = persistence concerns (columns, validation, relationships)
 *    - UserPrincipal = security concerns (authorities, credentials, status)
 *    - Each class has one clear responsibility
 * 
 * 4. Evolution:
 *    - Can change User entity without affecting security code
 *    - Can add security features without modifying persistence layer
 *    - Tests for User and UserPrincipal are independent
 * 
 * UserDetails Interface Contract:
 * Spring Security requires implementations of UserDetails to provide:
 * - getUsername(): Returns login identifier (email in our case)
 * - getPassword(): Returns hashed password (for authentication)
 * - getAuthorities(): Returns list of granted authorities (roles)
 * - isAccountNonExpired(): Whether account has expired
 * - isAccountNonLocked(): Whether account is locked
 * - isCredentialsNonExpired(): Whether password has expired
 * - isEnabled(): Whether account is active
 * 
 * Field Mapping:
 * - User.email → username (email used as login identifier)
 * - User.passwordHash → password (BCrypt hash, not plain text)
 * - User.role → authorities (converted to GrantedAuthority list)
 * - User.id → id (convenience accessor for getting numeric ID)
 * 
 * Authority Conversion:
 * - User.role = Role.ROLE_USER (enum)
 * - Converted to SimpleGrantedAuthority("ROLE_USER")
 * - Used by @PreAuthorize("hasRole('USER')") in controllers
 * - Spring Security checks role string against UserDetails authorities
 * 
 * Hardcoded Boolean Methods (Sprint 1):
 * The following methods return hardcoded true:
 * - isAccountNonExpired() → true
 * - isAccountNonLocked() → true
 * - isCredentialsNonExpired() → true
 * - isEnabled() → true
 * 
 * Why Hardcoded?
 * Sprint 1 doesn't include account status features:
 * - No account expiration dates
 * - No account lockout (brute-force protection comes in Sprint 2)
 * - No password expiration policies
 * - No account disabling/soft delete
 * 
 * Future Work (Sprint 2+):
 * When these features are implemented, update corresponding methods:
 * - Add locked_until timestamp to User
 * - Add password_changed_at timestamp for expiration
 * - Add enabled boolean flag
 * - Query these in corresponding isXxx() methods
 * - DO NOT keep hardcoded true (security bug!)
 * 
 * Usage in Application:
 * 1. CustomUserDetailsService.loadUserByUsername() creates UserPrincipal from User
 * 2. JwtUtil.extractRole() reads role from JWT token
 * 3. Spring Security stores UserPrincipal in SecurityContext during request
 * 4. @PreAuthorize / @Secured annotations check authorities
 * 5. SecurityContext retrieves principal for authorization checks
 * 6. After request, SecurityContext is cleared (stateless authentication)
 * 
 * Convenience Accessor:
 * getId() method provides numeric user ID for convenience.
 * Useful for:
 * - Embedding in JWT tokens (efficient numeric ID vs. email string)
 * - Logging user actions
 * - Distinguishing users in concurrent operations
 * - Reducing database queries (already have ID from principal)
 * 
 * @see org.springframework.security.core.userdetails.UserDetails
 * @see org.springframework.security.core.GrantedAuthority
 * @see com.expenseTracker.model.entity.User
 * @see com.expenseTracker.security.CustomUserDetailsService
 * @see com.expenseTracker.security.JwtUtil
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