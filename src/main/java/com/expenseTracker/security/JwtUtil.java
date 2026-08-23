package com.expenseTracker.security;

import com.expenseTracker.model.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Handles ONLY access tokens - short-lived, self-contained, signed JWTs that
 * are validated by signature + expiry alone (no DB round trip). Refresh
 * tokens are a deliberately different mechanism: opaque random strings,
 * hashed and stored in refresh_tokens, validated by DB lookup so they can be
 * revoked (RefreshTokenService, Day 5). Do not extend this class to also
 * issue refresh tokens - that would silently reintroduce non-revocable
 * sessions through the back door.
 */
@Component
public class JwtUtil {

    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey signingKey;
    private final Duration accessTokenExpiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.access.expiration}") Duration accessTokenExpiration) {
        // Keys.hmacShaKeyFor validates key length against the HMAC algorithm's
        // minimum (32 bytes for HS256) and throws WeakKeyException immediately
        // at startup if the configured secret is too short - fail fast, not on
        // the first login attempt in production.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
    }

    public String generateAccessToken(Long userId, String email, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenExpiration)))
                .signWith(signingKey) // algorithm auto-selected from key type/length (HS256 here)
                .compact();
    }

    /**
     * Throws JwtException (ExpiredJwtException, SignatureException,
     * MalformedJwtException, etc.) on any problem. Day 4's JwtAuthFilter
     * should catch broadly at the JwtException level and treat every failure
     * identically - "not authenticated" - rather than distinguishing reasons
     * back to the caller. Telling a client specifically "signature invalid"
     * vs. "token expired" hands an attacker a validation oracle for free.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return parseClaims(token).get(CLAIM_USER_ID, Long.class);
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Role extractRole(String token) {
        String roleClaim = parseClaims(token).get(CLAIM_ROLE, String.class);
        try {
            return Role.valueOf(roleClaim);
        } catch (IllegalArgumentException e) {
            // parseClaims already succeeded above, so signature/expiry are fine -
            // this is specifically a token whose role claim no longer maps to a
            // live enum constant (e.g. a role renamed during a rolling deploy
            // while some already-issued access tokens are still within their
            // 15-minute lifetime). Rethrowing as JwtException keeps this
            // method's failure contract identical to isTokenValid/parseClaims -
            // "any problem with this token -> JwtException family" - so Day 4's
            // JwtAuthFilter can catch one exception type uniformly instead of
            // needing to know this one call throws something different.
            throw new JwtException("Token contains unrecognized role claim: " + roleClaim, e);
        }
    }
}