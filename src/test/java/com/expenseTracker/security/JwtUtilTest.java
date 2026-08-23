package com.expenseTracker.security;

import com.expenseTracker.model.entity.Role;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pure unit test - JwtUtil is instantiated directly with a test secret, no
 * Spring context needed. This is the highest-value test in today's batch:
 * jjwt's builder/parser API changed shape across 0.11 -> 0.12 -> 0.13, so a
 * silent method-signature mismatch is exactly the kind of thing that looks
 * fine at compile time and breaks at runtime. Lightweight smoke coverage,
 * not the full Day 11 suite - no coverage here yet for clock-skew tolerance
 * or malformed-token shapes beyond a bad signature.
 */
class JwtUtilTest {

    // 32+ bytes, satisfies HS256's minimum key length (same requirement the
    // real jwt.secret property is subject to).
    private static final String TEST_SECRET = "test-only-secret-key-must-be-at-least-32-bytes-long";

    private final JwtUtil jwtUtil = new JwtUtil(TEST_SECRET, Duration.ofMinutes(15));

    @Test
    void generatedTokenRoundTripsAllClaims() {
        String token = jwtUtil.generateAccessToken(42L, "ada@example.com", Role.ROLE_ADMIN);

        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtUtil.extractEmail(token)).isEqualTo("ada@example.com");
        assertThat(jwtUtil.extractRole(token)).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    void tokenSignedWithADifferentSecretIsRejected() {
        JwtUtil otherIssuer = new JwtUtil("a-completely-different-secret-key-at-least-32-bytes", Duration.ofMinutes(15));
        String token = otherIssuer.generateAccessToken(1L, "eve@example.com", Role.ROLE_USER);

        assertThat(jwtUtil.isTokenValid(token)).isFalse();
        assertThatThrownBy(() -> jwtUtil.parseClaims(token)).isInstanceOf(SignatureException.class);
    }

    @Test
    void alreadyExpiredTokenIsRejected() {
        // Negative duration -> expiration is already in the past the instant
        // the token is minted. Deterministic, no Thread.sleep needed.
        JwtUtil expiredIssuer = new JwtUtil(TEST_SECRET, Duration.ofSeconds(-1));
        String token = expiredIssuer.generateAccessToken(1L, "ada@example.com", Role.ROLE_USER);

        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }

    @Test
    void malformedTokenIsRejected() {
        assertThat(jwtUtil.isTokenValid("not-a-real-jwt")).isFalse();
    }

    @Test
    void tokenWithUnrecognizedRoleClaimThrowsJwtException() {
        // Crafted directly with Jwts.builder(), not jwtUtil.generateAccessToken -
        // that method only accepts real Role enum values, so it can't produce
        // this shape itself. Simulates a token minted before a role was
        // renamed/removed, e.g. mid rolling-deploy, where some already-issued
        // access tokens still carry a role that no longer exists as a constant.
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject("ada@example.com")
                .claim("userId", 1L)
                .claim("role", "ROLE_LEGACY_UNKNOWN")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofMinutes(15))))
                .signWith(key)
                .compact();

        // Signature and expiry are both fine - only the role claim is bad -
        // so this must fail via extractRole's own handling, not signature
        // verification.
        assertThatThrownBy(() -> jwtUtil.extractRole(token)).isInstanceOf(JwtException.class);
    }
}