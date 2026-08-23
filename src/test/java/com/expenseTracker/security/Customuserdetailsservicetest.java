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
 * Pure unit test - UserRepository is mocked, no Spring context, no DB.
 * This is a lightweight smoke test to de-risk today's work, not the
 * comprehensive AuthService/JwtUtil/RefreshTokenService suite the tracker
 * schedules for Day 11 - that pass should add the edge cases this doesn't
 * cover (e.g. disabled/locked accounts, once those states exist).
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