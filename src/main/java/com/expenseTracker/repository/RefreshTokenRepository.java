package com.expenseTracker.repository;

import com.expenseTracker.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for RefreshToken entity.
 * 
 * This repository handles database operations for refresh tokens. It provides methods for:
 * - Finding tokens by hash (for validation)
 * - Finding all active tokens for a user (for token rotation)
 * - Revoking tokens individually or in bulk (for security events)
 * 
 * Inherited Methods (from JpaRepository<RefreshToken, Long>):
 * - save(RefreshToken token): Persist a new token
 * - findById(Long id): Find token by primary key
 * - delete(RefreshToken token): Delete a single token
 * - deleteById(Long id): Delete by primary key
 * - findAll(): Retrieve all tokens (rarely useful)
 * - flush(): Force pending changes to database
 * 
 * Custom Query Methods:
 * 
 * 1. findByTokenHash(String tokenHash)
 *    Purpose: Validate a refresh token during token refresh request
 *    Returns: Optional<RefreshToken> (empty if not found or hash doesn't match)
 *    Usage: RefreshTokenService looks up token hash from httpOnly cookie
 *    Security: Uses hashed token, not the raw token value
 *    Performance: Uses database index on token_hash column
 * 
 * 2. findAllByUserIdAndRevokedFalse(Long userId)
 *    Purpose: List all active (not revoked) refresh tokens for a user
 *    Returns: List<RefreshToken> (empty list if none or all revoked)
 *    Usage: "Log out everywhere" feature, checking active sessions
 *    Filter Condition: Only returns tokens where is_revoked = false
 *    Note: Does not filter by expiration (caller handles that separately)
 * 
 * 3. revokeAllByUserId(Long userId)
 *    Purpose: Immediately invalidate all active tokens for a user
 *    Returns: int (count of tokens that were revoked)
 *    Usage: Password change, account lockout, admin-initiated logout
 *    Implementation: Bulk UPDATE query (not loading individual rows)
 * 
 *    Why Bulk UPDATE?
 *    - Direct SQL UPDATE statement (no JPA entity loading)
 *    - Revokes hundreds/thousands of tokens in one operation
 *    - Much faster than loading each token, modifying, and re-saving
 *    - @Modifying tells Spring to execute as DML, not query
 *    - @Transactional required (auto-applied by Spring Data)
 *    - Returned int = number of rows updated
 * 
 * Revocation Use Cases (Sprint 1):
 * - User clicks "Log out" → Revoke current token only
 * - User clicks "Log out everywhere" → Revoke all tokens (revokeAllByUserId)
 * - User changes password → Revoke all tokens (revokeAllByUserId)
 * - User's account is disabled → Revoke all tokens (revokeAllByUserId)
 * - Token exceeds expiration time → Naturally invalid (no explicit revoke needed)
 * 
 * Security Design:
 * - Tokens are revocable (stateful, database-backed)
 * - Unlike JWT which cannot be revoked until expiration
 * - Allows immediate logout even if access token is still valid
 * - Enables password change to immediately invalidate all sessions
 * - Admin can revoke user's sessions without user interaction
 * 
 * Performance Considerations:
 * - Query methods benefit from database indexes:
 *   * Index on token_hash (for fast token validation)
 *   * Index on user_id (for finding user's tokens)
 *   * Composite index on (user_id, is_revoked) for the WHERE clause
 * - @Modifying query counts rows updated (watch for N+1 revocation patterns)
 * - Bulk revocation is single database roundtrip
 * - Spring Data JPA automatically adds pagination/sorting support
 * 
 * Testing Considerations:
 * - AuthRepositoryIT provides integration tests with real database
 * - Uses TestContainers for isolated MySQL environment
 * - Tests cascade delete (user deletion removes tokens)
 * - Tests bulk revocation (only affects target user)
 * - Tests expiry logic with fixed Instant values (no Thread.sleep)
 * 
 * @see com.expenseTracker.model.entity.RefreshToken
 * @see com.expenseTracker.model.entity.User
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Find a refresh token by its token hash.
     * 
     * Used during the token refresh request to look up the token in the database.
     * The token value is never stored plain (only the SHA-256 hash), so the
     * hash is compared against what's stored.
     * 
     * @param tokenHash the SHA-256 hash of the refresh token
     * @return Optional containing the RefreshToken if found and valid, empty Optional otherwise
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Find all active (not revoked) refresh tokens for a specific user.
     * 
     * Used for listing a user's active sessions or implementing "log out everywhere".
     * Does not filter by expiration time - caller should check isUsable() on results.
     * 
     * @param userId the ID of the user
     * @return List of active RefreshToken objects (empty if none exist)
     */
    List<RefreshToken> findAllByUserIdAndRevokedFalse(Long userId);

    /**
     * Bulk-revoke every active refresh token for a user.
     * 
     * Used for:
     * - "Log out everywhere" feature
     * - Password change (invalidates all existing sessions)
     * - Account lockout/suspension
     * - Forcing re-authentication after privilege elevation
     * 
     * Implementation Details:
     * - Uses bulk UPDATE query (efficient, single database roundtrip)
     * - Only affects tokens where is_revoked = false (already-revoked tokens unchanged)
     * - Returns count of tokens that were actually revoked
     * - Immediate effect (caller doesn't need to reload tokens)
     * 
     * @param userId the ID of the user whose tokens should be revoked
     * @return the number of tokens that were revoked (updated from false to true)
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);
}