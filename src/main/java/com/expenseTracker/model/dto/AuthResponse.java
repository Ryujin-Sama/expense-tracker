package com.expenseTracker.model.dto;

import com.expenseTracker.model.entity.User;

/**
 * Data Transfer Object (DTO) for authentication responses.
 * 
 * This class represents the data returned to the client after successful authentication.
 * It contains user profile information but intentionally DOES NOT include authentication tokens.
 * 
 * Design Decision - No Token Fields:
 * This class deliberately carries NO token fields (accessToken, refreshToken, etc.).
 * 
 * Why? Under the httpOnly cookie-based auth model (Secure, SameSite=Strict):
 * - Tokens travel EXCLUSIVELY via Set-Cookie HTTP headers
 * - Tokens never appear in the JSON response body
 * - This keeps tokens out of reach of JavaScript code (preventing XSS attacks)
 * - Putting tokens in both cookies AND JSON response would:
 *   * Expose tokens to JavaScript via JSON parsing
 *   * Partially defeat the entire reason for choosing httpOnly cookies
 *   * Create two sources of truth for the same token
 *   * Increase surface area for security vulnerabilities
 * 
 * Token Flow (Sprint 1):
 * - Client sends credentials via POST /api/auth/login (AuthRequest)
 * - AuthService validates credentials
 * - AuthController generates tokens via JwtUtil
 * - AuthController sets tokens in response headers (Set-Cookie)
 * - Client receives AuthResponse with user data
 * - Client receives tokens in cookies (automatic browser handling)
 * - Subsequent requests include token via cookie (automatic browser handling)
 * 
 * Fields Included:
 * - id: User's unique database identifier
 * - email: User's email address (username)
 * - firstName: User's first name
 * - lastName: User's last name
 * - baseCurrency: User's preferred currency for expense tracking (e.g., USD)
 * - role: User's authorization role (ROLE_USER or ROLE_ADMIN)
 * 
 * Usage:
 * - Returned by POST /api/auth/login on successful authentication
 * - Returned by POST /api/auth/register on successful account creation
 * - Client displays user info in UI
 * - Client uses role to determine available features
 * 
 * @see com.expenseTracker.model.entity.User
 * @see com.expenseTracker.security.JwtUtil
 */
public class AuthResponse {

    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String baseCurrency;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(Long id, String email, String firstName, String lastName,
                        String baseCurrency, String role) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.baseCurrency = baseCurrency;
        this.role = role;
    }

    public static AuthResponse fromUser(User user) {
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getBaseCurrency(),
                user.getRole().name());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}