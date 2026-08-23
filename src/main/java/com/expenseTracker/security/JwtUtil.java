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
 * JWT (JSON Web Token) Utility for access token generation and validation.
 * 
 * This component handles ONLY SHORT-LIVED ACCESS TOKENS (15 minute expiry).
 * It is deliberately NOT responsible for refresh tokens (see RefreshToken entity).
 * 
 * Token Types in This System:
 * 
 * Access Token (Handled HERE by JwtUtil):
 * - Short-lived (15 minutes default)
 * - Self-contained, signed JWT
 * - Includes: userId, email, role, issuedAt, expiresAt
 * - Validated by: signature verification + expiry time check
 * - No database lookup needed (stateless validation)
 * - Used for authorizing API requests
 * - Secure to store in httpOnly cookie
 * 
 * Refresh Token (Deliberately NOT handled here):
 * - Long-lived (7 days)
 * - Opaque random string (not JWT)
 * - Never self-contained (must check database)
 * - Stored as SHA-256 hash in refresh_tokens table
 * - Validated by: database lookup + revocation check
 * - Can be revoked without affecting other tokens
 * - Used to obtain new access tokens without re-authentication
 * - Secure to store in httpOnly cookie
 * 
 * Why Separate Mechanisms?
 * Using JWT for refresh tokens would be a SECURITY ANTI-PATTERN:
 * - JWTs cannot be revoked (stateless)
 * - A compromised refresh token would be valid until expiration
 * - If user changes password, old refresh tokens stay valid
 * - Attacker could indefinitely refresh new access tokens
 * - Bulk logout ("log out everywhere") becomes impossible
 * 
 * This design intentionally keeps refresh token logic OUT of JwtUtil
 * to prevent accidentally combining both mechanisms and reintroducing
 * non-revocable sessions through the back door.
 * 
 * JWT Claims (Payload):
 * - sub (subject): User's email address (login identifier)
 * - userId: User's numeric database ID
 * - role: User's role (ROLE_USER or ROLE_ADMIN)
 * - iat (issuedAt): Token creation timestamp
 * - exp (expiration): Token expiration timestamp
 * 
 * Signing Algorithm:
 * - HS256 (HMAC-SHA-256)
 * - Symmetric key (both signing and verification use same secret)
 * - Secret length must be >= 32 bytes (HS256 minimum)
 * - Configurable via jwt.secret property
 * 
 * Token Validation Strategy:
 * 1. Parse token with secret key (signature verification)
 * 2. Extract claims (if parsing succeeds, signature is valid)
 * 3. Check expiration time (Instant.now() > exp)
 * 4. Extract specific claims (userId, role, email)
 * 
 * Exception Handling:
 * JwtUtil throws JwtException (or subclasses) on any validation failure:
 * - io.jsonwebtoken.ExpiredJwtException: Token has expired
 * - io.jsonwebtoken.security.SignatureException: Signature doesn't match
 * - io.jsonwebtoken.MalformedJwtException: Token format is invalid
 * - io.jsonwebtoken.UnsupportedJwtException: Token uses unsupported algorithm
 * - io.jsonwebtoken.IllegalArgumentException: Null or empty token
 * 
 * Caller Responsibility:
 * Callers (like JwtAuthFilter) MUST catch JwtException broadly.
 * Do NOT expose specific exception types to client:
 * - ✓ Catch: JwtException → 401 Unauthorized
 * - ✗ Avoid: Distinguish ExpiredJwtException vs. SignatureException
 * - ✗ Avoid: Return "token expired" vs. "invalid signature"
 * - Reason: Revealing validation failure type is information leakage
 * 
 * Configuration:
 * - jwt.secret: HMAC signing secret (must be >= 32 bytes)
 * - jwt.access.expiration: Access token lifetime (e.g., "15m")
 * - Both are injected from application.yml or environment variables
 * - Key validation happens at construction (fail-fast at startup)
 * 
 * Usage Example:
 * 1. On successful login:
 *    String token = jwtUtil.generateAccessToken(userId, email, role);
 * 
 * 2. On API request:
 *    String token = extractTokenFromCookie();
 *    if (jwtUtil.isTokenValid(token)) {
 *        Long userId = jwtUtil.extractUserId(token);
 *        // Use userId for authorization
 *    }
 * 
 * Testing:
 * - JwtUtilTest provides unit tests
 * - Tests token round-tripping (generate → extract claims)
 * - Tests signature validation (different secret → rejection)
 * - Tests expiration (negative duration → already expired)
 * - Tests malformed tokens (invalid format → rejection)
 * - No Spring context needed (direct instantiation)
 * 
 * @see com.expenseTracker.model.entity.RefreshToken
 * @see com.expenseTracker.security.UserPrincipal
 * @see com.expenseTracker.repository.RefreshTokenRepository
 * @version 0.1.0
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