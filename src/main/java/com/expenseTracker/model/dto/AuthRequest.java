package com.expenseTracker.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for user login requests.
 * 
 * This class captures the credentials needed for user authentication via the login endpoint.
 * It contains email and password fields with validation constraints to ensure data integrity.
 * 
 * Security Considerations:
 * - Deliberately NO password complexity validation here (unlike RegisterRequest)
 * - Password policy is enforced once at registration time only
 * - At login, a wrong password should fail authentication generically
 * - Validating complexity here would allow attackers to distinguish "malformed" from "wrong"
 * - Rejecting legitimate old passwords if policy tightens later would cause customer support issues
 * - Password size is bounded to 128 bytes as a DoS mitigation measure (Sprint 1 has no rate-limiting)
 * 
 * Validation Rules:
 * - email: Required, must be valid email format
 * - password: Required, max 128 characters (bounded to prevent BCrypt computation abuse)
 * 
 * Usage Flow:
 * 1. Client submits credentials via POST /api/auth/login
 * 2. Spring validates constraints before controller receives the object
 * 3. AuthService validates credentials against stored password hash
 * 4. On success, server returns AccessToken + RefreshToken (via httpOnly cookies)
 * 
 * @see com.expenseTracker.model.dto.RegisterRequest
 * @see com.expenseTracker.model.dto.AuthResponse
 */
public class AuthRequest {

    @NotBlank
    @Email
    private String email;

    // Same reasoning as RegisterRequest.password - unauthenticated endpoint,
    // no rate-limiting until Sprint 2, unbounded input hits BCrypt.matches()
    // on every attempt.
    @NotBlank
    @Size(max = 128)
    private String password;

    public AuthRequest() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}