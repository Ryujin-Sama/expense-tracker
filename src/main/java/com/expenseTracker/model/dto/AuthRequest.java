package com.expenseTracker.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request. Deliberately no password complexity validation here (unlike
 * RegisterRequest) - the policy is enforced once, at registration. At login,
 * a wrong password should just fail authentication generically; validating
 * complexity here would let an attacker distinguish "malformed" from
 * "wrong" and would reject legitimate old passwords if the policy tightens
 * later.
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