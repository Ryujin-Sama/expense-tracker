package com.expenseTracker.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Refresh token entity for implementing token rotation and revocation.
 * 
 * This entity stores hashed refresh tokens that can be revoked without requiring
 * clients to immediately log in again. Refresh tokens are long-lived (7 days),
 * whereas access tokens are short-lived (15 minutes).
 * 
 * Design Principles:
 * - Tokens are NEVER stored in plain text (always hashed with SHA-256)
 * - Token revocation is database-backed (unlike JWT which is stateless)
 * - Tokens can be individually revoked or bulk-revoked (e.g., on password change)
 * - Lazy-loading User relationship (we rarely need full user data for token validation)
 * 
 * JPA Entity Mapping:
 * - Table Name: "refresh_tokens"
 * - Primary Key: id (auto-generated IDENTITY)
 * - Foreign Key: user_id (references users.id, cascading delete)
 * 
 * Attributes:
 * - id: Auto-generated primary key
 * - user: Lazy-loaded reference to the User who owns this token
 * - tokenHash: SHA-256 hash of the raw opaque token (128-character hex string)
 * - expiresAt: Timestamp when token becomes invalid (default 7 days from creation)
 * - revoked: Flag indicating if token has been explicitly revoked (false = active)
 * - createdAt: Timestamp of token creation (set automatically via @PrePersist)
 * 
 * Database Columns (and Java Mapping):
 * - user_id: Foreign key to users.id (NOT NULL, CASCADE DELETE)
 * - token_hash: Unique hash (NOT NULL, UNIQUE, length 255)
 * - expiry_date: Expiration timestamp (NOT NULL)
 * - is_revoked: Revocation flag (NOT NULL, default false)
 * - created_at: Creation timestamp (NOT NULL, IMMUTABLE)
 * 
 * Token Validation Logic:
 * A refresh token is usable if and only if:
 * 1. is_revoked = false (not explicitly revoked)
 * 2. expiry_date > now() (not yet expired)
 * 
 * Methods:
 * - isExpired(Instant now): Check if token has passed its expiration time
 * - isUsable(Instant now): Check if token is both not revoked AND not expired
 * - isExpired(): Convenience method using Instant.now()
 * - isUsable(): Convenience method using Instant.now()
 * 
 * Revocation Scenarios (Sprint 1):
 * - User explicitly logs out
 * - Password is changed (all existing refresh tokens revoked)
 * - User's account is disabled/deleted
 * - Admin revokes user's sessions
 * 
 * Token Flow:
 * 1. User logs in successfully
 * 2. AuthService generates opaque random token (e.g., 64-byte random)
 * 3. SHA-256 hash is computed and stored in token_hash column
 * 4. Raw token is returned to client (in httpOnly cookie)
 * 5. Client includes cookie in requests automatically
 * 6. Server validates token hash against stored hash
 * 7. If still usable, server issues new access token
 * 8. Process repeats until refresh token expires or is revoked
 * 
 * Security Notes:
 * - Raw token is never persisted (only hash)
 * - If database is breached, attacker cannot use stolen token hashes
 * - Tokens are bound to specific users (via user_id foreign key)
 * - Cascading delete: removing a user also removes their tokens
 * - Test data uses fixed Instant values, but production uses Instant.now()
 * 
 * @see com.expenseTracker.model.entity.User
 * @see com.expenseTracker.security.JwtUtil
 * @see com.expenseTracker.repository.RefreshTokenRepository
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lazy - we almost never need the full User loaded just to validate a
    // refresh token; JwtAuthFilter-adjacent code cares about the FK, not the
    // profile fields.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 hash of the raw opaque refresh token - the raw value is never persisted. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    // Java field stays "expiresAt" (reads better in code, and JPQL/tests
    // reference this attribute name, not the column) - only the physical
    // column name follows the FDS v1.2 naming (expiry_date).
    @Column(name = "expiry_date", nullable = false)
    private Instant expiresAt;

    // Same story - column is is_revoked per the FDS, Java field/accessor
    // naming (revoked / isRevoked()) is unaffected.
    @Column(name = "is_revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {
    }

    public RefreshToken(User user, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    /**
     * Takes an explicit "now" rather than reading a Clock field off the entity -
     * entities that hold live collaborators are awkward to construct, compare,
     * and serialize. This keeps expiry logic pure and trivially testable
     * (isExpired(fixedInstant)) without changing how the entity is built.
     */
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isExpired() {
        return isExpired(Instant.now());
    }

    public boolean isUsable(Instant now) {
        return !revoked && !isExpired(now);
    }

    public boolean isUsable() {
        return isUsable(Instant.now());
    }

    // --- Getters and setters ---

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefreshToken)) return false;
        RefreshToken that = (RefreshToken) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        // Excludes tokenHash - no reason for it to ever land in a log line.
        return "RefreshToken{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", expiresAt=" + expiresAt +
                ", revoked=" + revoked +
                ", createdAt=" + createdAt +
                '}';
    }
}