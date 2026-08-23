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