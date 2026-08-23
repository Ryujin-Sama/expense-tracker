package com.expenseTracker.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    // FDS: "min 8 characters, alphanumeric" - interpreted as requiring at
    // least one letter AND one digit, not merely allowing them. A single
    // @Pattern covers length + composition in one clear validation message
    // rather than splitting into competing @Size/@Pattern messages.
    // Upper bound matters here specifically because register/login are
    // unauthenticated and rate-limiting doesn't land until Sprint 2 - an
    // unbounded string gets fed straight into BCrypt.encode() on every
    // request. 128 is well above any real password but closes the gap now.
    @NotBlank
    @Size(max = 128)
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
            message = "Password must be at least 8 characters long and contain both letters and numbers")
    private String password;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    // Optional - null/blank is valid (entity defaults to USD). If present,
    // must be a 3-letter uppercase ISO 4217-shaped code. This does NOT
    // validate against the actual ISO 4217 list (e.g. "ZZZ" would pass) -
    // that's a currency-lookup-table concern for later, not Sprint 1 scope.
    @Pattern(regexp = "^$|[A-Z]{3}$", message = "Base currency must be a 3-letter ISO 4217 code (e.g. USD)")
    private String baseCurrency;

    public RegisterRequest() {
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
}