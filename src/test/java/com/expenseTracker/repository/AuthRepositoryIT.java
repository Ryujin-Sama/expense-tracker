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
 * Integration tests for User and RefreshToken repositories.
 * 
 * Test Type: Integration Tests (IT)
 * These tests run against a REAL database (MySQL in a container), not an in-memory H2.
 * They verify that:
 * - Flyway migrations execute correctly
 * - Entity mappings match the database schema
 * - JPA queries generate correct SQL
 * - Database constraints are enforced
 * - Cascade delete and foreign keys work as expected
 * 
 * Why Use a Real Database?
 * Default @DataJpaTest uses embedded H2 database which:
 * - Silently skips MySQL-specific migrations (ON UPDATE CURRENT_TIMESTAMP, etc.)
 * - Supports different SQL syntax than MySQL
 * - Misses constraint issues until production
 * - Allows tests to pass that would fail in production
 * 
 * TestContainers Solution:
 * - Spawns a real MySQL 8.4 container for each test run
 * - Runs Flyway migrations against this container
 * - Verifies all migration syntax and constraints
 * - Cleans up container after tests complete
 * - Isolates tests from each other (fresh database per test class)
 * 
 * Test Configuration:
 * - @DataJpaTest: Loads Spring Data JPA configuration + repositories
 * - @Testcontainers: Enables TestContainers framework
 * - @AutoConfigureTestDatabase(replace = NONE): Use real DB, don't replace with H2
 * - @Container: Static MySQL container instance
 * - @DynamicPropertySource: Inject container connection details into Spring
 * 
 * Test Coverage:
 * 
 * 1. User Entity & Repository:
 *    - User can be saved and retrieved
 *    - Email unique constraint is enforced
 *    - Duplicate email throws DataIntegrityViolationException
 *    - existsByEmail() works for both existing and non-existing emails
 *    - createdAt timestamp is automatically set
 * 
 * 2. RefreshToken Entity & Repository:
 *    - Cascade delete works (user deleted -> tokens deleted)
 *    - Bulk revocation only affects target user's tokens
 *    - Bulk revocation returns correct count
 *    - Revocation flag is correctly persisted
 *    - Already-revoked tokens are not re-revoked
 * 
 * 3. Token Expiry Logic:
 *    - isExpired(Instant) correctly evaluates expiration
 *    - isUsable(Instant) combines revocation + expiration checks
 *    - Both methods work with fixed test Instants (deterministic)
 * 
 * Important Flyway Migrations Tested:
 * - V1__init_Users_Table.sql
 * - V2__Init_Refresh_Tokens_Table.sql
 * 
 * These migrations are applied automatically at test startup.
 * If migration syntax is wrong or incompatible, tests fail immediately.
 * 
 * Performance Considerations:
 * - Full container startup takes ~5-10 seconds per test class
 * - Acceptable for CI/CD validation
 * - Not suitable for "watch mode" rapid feedback
 * - Can be skipped locally with -Dskip.docker=true if needed
 * 
 * Debugging Failed Tests:
 * If tests fail:
 * 1. Check migration SQL syntax (Flyway errors are detailed)
 * 2. Verify entity annotations match database schema
 * 3. Check database constraint definitions
 * 4. Look at MySQL container logs (Docker logs)
 * 5. Ensure LocalDateTime vs Instant field types match
 * 
 * Test Isolation:
 * - Each test runs in its own transaction
 * - @Transactional(readOnly=true) rolls back after test
 * - No data persists between tests
 * - Multiple tests can run in parallel safely
 * - Database is isolated from other test classes
 * 
 * @see com.expenseTracker.repository.UserRepository
 * @see com.expenseTracker.repository.RefreshTokenRepository
 * @see com.expenseTracker.model.entity.User
 * @see com.expenseTracker.model.entity.RefreshToken
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