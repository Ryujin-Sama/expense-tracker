package com.expenseTracker.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for user registration requests.
 * 
 * This class captures all information needed to create a new user account. Each field has
 * validation constraints to ensure data quality and security before the request reaches the
 * business logic layer.
 * 
 * Field Descriptions:
 * - email: Required, must be valid email format, max 255 characters (unique in database)
 * - password: Required, must be at least 8 characters with both letters and numbers
 * - firstName: Required, max 100 characters
 * - lastName: Required, max 100 characters  
 * - baseCurrency: Optional, defaults to USD if not provided, must be 3-letter ISO 4217 code
 * 
 * Security Considerations:
 * - Password complexity is enforced at registration (unlike login - see AuthRequest)
 * - Single @Pattern annotation covers both length and composition for clearer error messages
 * - Password max length is 128 bytes because registration is unauthenticated:
 *   * Sprint 1 has no rate-limiting protection
 *   * Unbounded input would be fed directly into BCrypt.encode() on every request
 *   * BCrypt is intentionally slow for security; large inputs amplify DoS risk
 * - Email is bounded to 255 characters (database column constraint)
 * - Currency code validation is format-only, not against ISO 4217 master list
 *   (Currency lookup tables are future scope, not Sprint 1)
 * - All fields are validated BEFORE business logic runs (fail-fast principle)
 * 
 * Password Requirements:
 * Pattern: ^(?=.*[A-Za-z])(?=.*\\d).{8,}$
 * - At least 8 characters
 * - Contains at least one letter (A-Z, a-z)
 * - Contains at least one digit (0-9)
 * - No special characters required (OWASP guidance)
 * 
 * Usage Flow:
 * 1. Client submits registration data via POST /api/auth/register
 * 2. Spring's Bean Validation validates all @Constraint annotations
 * 3. If validation fails, 400 Bad Request with error details is returned
 * 4. If valid, AuthService receives RegisterRequest and creates new User entity
 * 5. Password is hashed with BCrypt before storage in database
 * 6. New user is assigned ROLE_USER by default (cannot be changed via registration)
 * 
 * @see com.expenseTracker.model.dto.AuthRequest
 * @see com.expenseTracker.model.dto.AuthResponse
 * @see com.expenseTracker.model.entity.User
 */
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