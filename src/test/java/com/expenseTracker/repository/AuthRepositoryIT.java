package com.expenseTracker.repository;

import com.expenseTracker.model.entity.RefreshToken;
import com.expenseTracker.model.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real V1/V2 Flyway migrations against a throwaway MySQL container
 * and exercises both repositories against them. Exists to catch
 * entity/schema drift (a renamed column, a dropped constraint, a
 * misconfigured FK) at build time instead of at Day 4 when AuthService
 * starts writing real rows.
 *
 * replace = Replace.NONE is required - by default @DataJpaTest swaps in an
 * embedded H2 database, which would silently skip the MySQL-specific
 * migrations (ON UPDATE CURRENT_TIMESTAMP, the FK CASCADE) entirely.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuthRepositoryIT {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("expense_tracker_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Test
    void savesUserAndEnforcesUniqueEmail() {
        User saved = userRepository.save(new User("ada@example.com", "hashed-password", "Ada", "Lovelace", "USD"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(userRepository.findByEmail("ada@example.com")).isPresent();
        assertThat(userRepository.existsByEmail("ada@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();

        User duplicate = new User("ada@example.com", "another-hash", "Grace", "Hopper", "USD");
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void refreshTokenCascadesOnUserDeletion() {
        User user = userRepository.save(new User("cascade@example.com", "hash", "Alan", "Turing", "USD"));
        RefreshToken token = refreshTokenRepository.save(
                new RefreshToken(user, "some-token-hash", Instant.now().plusSeconds(3600)));

        userRepository.delete(user);
        userRepository.flush();

        assertThat(refreshTokenRepository.findById(token.getId())).isEmpty();
    }

    @Test
    void revokeAllByUserIdOnlyTouchesThatUsersActiveTokens() {
        User owner = userRepository.save(new User("revoke@example.com", "hash", "Katherine", "Johnson", "USD"));
        User otherUser = userRepository.save(new User("other@example.com", "hash", "Dorothy", "Vaughan", "USD"));

        RefreshToken active = refreshTokenRepository.save(
                new RefreshToken(owner, "hash-active", Instant.now().plusSeconds(3600)));

        RefreshToken alreadyRevoked = new RefreshToken(owner, "hash-already-revoked", Instant.now().plusSeconds(3600));
        alreadyRevoked.setRevoked(true);
        refreshTokenRepository.save(alreadyRevoked);

        RefreshToken otherUsersToken = refreshTokenRepository.save(
                new RefreshToken(otherUser, "hash-other-user", Instant.now().plusSeconds(3600)));

        int updatedCount = refreshTokenRepository.revokeAllByUserId(owner.getId());
        refreshTokenRepository.flush();

        assertThat(updatedCount).isEqualTo(1); // only the one still-active token for `owner`
        assertThat(refreshTokenRepository.findById(active.getId()).orElseThrow().isRevoked()).isTrue();
        assertThat(refreshTokenRepository.findById(otherUsersToken.getId()).orElseThrow().isRevoked()).isFalse();
    }

    @Test
    void refreshTokenExpiryIsEvaluatedAgainstTheGivenInstant() {
        User user = userRepository.save(new User("expiry@example.com", "hash", "Radia", "Perlman", "USD"));
        RefreshToken token = new RefreshToken(user, "hash-expiry", Instant.parse("2026-01-01T00:00:00Z"));

        assertThat(token.isExpired(Instant.parse("2025-12-31T23:59:59Z"))).isFalse();
        assertThat(token.isExpired(Instant.parse("2026-01-01T00:00:01Z"))).isTrue();
        assertThat(token.isUsable(Instant.parse("2025-12-31T23:59:59Z"))).isTrue();

        token.setRevoked(true);
        assertThat(token.isUsable(Instant.parse("2025-12-31T23:59:59Z")))
                .as("revoked tokens are never usable, even before expiry")
                .isFalse();
    }
}