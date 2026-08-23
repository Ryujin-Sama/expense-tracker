package com.expenseTracker.model.dto;

import com.expenseTracker.model.entity.User;

/**
 * Deliberately carries no token fields. Under the cookie-based auth model
 * (httpOnly, Secure, SameSite=Strict), both tokens travel exclusively via
 * Set-Cookie headers set by AuthController (Day 4/5) - never in the JSON
 * response body. Putting the token here as well as in a cookie would
 * partially defeat the reason for choosing httpOnly cookies in the first
 * place: keeping the token out of reach of JavaScript (and therefore XSS).
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