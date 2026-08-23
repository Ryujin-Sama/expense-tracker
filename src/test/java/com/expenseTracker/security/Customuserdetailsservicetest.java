package com.expenseTracker.security;

import com.expenseTracker.model.entity.Role;
import com.expenseTracker.model.entity.User;
import com.expenseTracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomUserDetailsService (Spring Security UserDetailsService).
 * 
 * Test Scope:
 * This is a pure unit test using mocking:
 * - UserRepository is mocked (no real database)
 * - No Spring context needed
 * - No database connections
 * - Tests only the UserDetailsService logic
 * 
 * Why Mock the Repository?
 * - Focus on CustomUserDetailsService behavior in isolation
 * - Tests don't depend on database state
 * - Tests run fast (no I/O)
 * - Tests are deterministic and reproducible
 * - Can easily test both success and failure paths
 * 
 * Test Coverage (Current - Sprint 1):
 * - Successful user loading and role mapping
 * - Exception thrown when user not found
 * - UserPrincipal correctly created from User entity
 * - Authorities list correctly populated with role
 * 
 * NOT Covered (Future Sprints):
 * - Account lockout/expiration handling (when those features exist)
 * - Disabled account handling (when soft-delete is implemented)
 * - Password expiration handling (when password policy is added)
 * - Multi-tenant scenarios (if application becomes multi-tenant)
 * - Performance/concurrent loading (load testing)
 * 
 * Critical Note - UsernameNotFoundException:
 * This test verifies that UsernameNotFoundException is thrown when user not found.
 * HOWEVER, this exception MUST NOT reach the client as a distinct error!
 * 
 * GlobalExceptionHandler (to be implemented in Sprint 4) must catch this and
 * return generic "Invalid email or password" to prevent user enumeration attacks.
 * This test only verifies the exception is thrown, not how it's handled.
 * 
 * Test Implementation Details:
 * - User is created as a transient entity (not persisted, no ID)
 * - Mockito.when() sets up mock behavior
 * - UserPrincipal.fromUser() is called by the service
 * - Assertions verify both UserDetails contract and role mapping
 * 
 * @see com.expenseTracker.security.CustomUserDetailsService
 * @see com.expenseTracker.security.UserPrincipal
 * @see org.springframework.security.core.userdetails.UserDetailsService
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void loadsUserAndMapsRoleToAuthority() {
        User user = new User("ada@example.com", "hashed-password", "Ada", "Lovelace", "USD");
        user.setRole(Role.ROLE_ADMIN);
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);
        UserDetails principal = service.loadUserByUsername("ada@example.com");

        assertThat(principal.getUsername()).isEqualTo("ada@example.com");
        assertThat(principal.getPassword()).isEqualTo("hashed-password");
        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ADMIN");
        assertThat(((UserPrincipal) principal).getId()).isNull(); // transient entity, never persisted
    }

    @Test
    void throwsUsernameNotFoundForUnknownEmail() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        CustomUserDetailsService service = new CustomUserDetailsService(userRepository);

        assertThatThrownBy(() -> service.loadUserByUsername("nobody@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
        // Deliberately not asserting on the exception message content in this
        // test - that message must never reach the client (see the class-level
        // comment on CustomUserDetailsService), so nothing should depend on it
        // beyond "this exception type was thrown."
    }
}