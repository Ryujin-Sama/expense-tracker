package com.expenseTracker.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * User entity representing a registered user in the Expense Tracker system.
 * 
 * JPA Entity Mapping:
 * - Table Name: "users"
 * - Primary Key: id (auto-generated IDENTITY)
 * - Email: Unique constraint enforced at database level
 * 
 * User Attributes:
 * - id: Auto-generated primary key
 * - email: User's unique email address (username for login)
 * - passwordHash: BCrypt-hashed password (never stored plain)
 * - firstName: User's first name
 * - lastName: User's last name
 * - baseCurrency: Preferred currency for expense tracking (defaults to USD)
 * - role: Authorization role - ROLE_USER (default) or ROLE_ADMIN
 * - createdAt: Timestamp of account creation (set automatically via @PrePersist)
 * 
 * Security Design:
 * - Password is stored as BCrypt hash ONLY - never plain text
 * - The entity never performs hashing - it only stores pre-hashed values
 * - BCrypt hashing is the responsibility of AuthService (encapsulation)
 * - Email is unique at database level (unique constraint)
 * 
 * Role-Based Access Control:
 * - ROLE_USER: Regular user who can manage their own expenses
 * - ROLE_ADMIN: Administrator with elevated privileges
 * - New registrations always default to ROLE_USER
 * - Role elevation only happens via administrative endpoints (not yet implemented)
 * 
 * Database Constraints:
 * - email: NOT NULL, UNIQUE, length 255
 * - password_hash: NOT NULL, length 255 (BCrypt produces 60-character hashes)
 * - first_name: NOT NULL, length 100
 * - last_name: NOT NULL, length 100
 * - base_currency: NOT NULL, length 3, default 'USD'
 * - role: NOT NULL, length 50, stored as ENUM string
 * - created_at: NOT NULL, IMMUTABLE (never updated, set only on insert)
 * 
 * Entity Lifecycle:
 * - Construction: Use constructor with required fields + optional baseCurrency
 * - Persistence: Call userRepository.save(user)
 * - onCreate() hook: @PrePersist automatically sets createdAt timestamp
 * - Retrieval: Use userRepository.findByEmail() or findById()
 * - Updates: Modify entity, call repository.save() or just let transaction commit
 * 
 * Equality & Hashing:
 * - Compared by database ID only (id-based equality)
 * - Transient objects (not yet persisted) are never equal
 * - toString() deliberately excludes passwordHash for security
 * 
 * Related Entities:
 * - RefreshToken: One-to-Many relationship (one user can have multiple refresh tokens)
 * - One user can have multiple expenses (not yet modeled in Sprint 1)
 * 
 * @see com.expenseTracker.model.entity.RefreshToken
 * @see com.expenseTracker.model.entity.Role
 * @see com.expenseTracker.model.dto.RegisterRequest
 * @see com.expenseTracker.model.dto.AuthResponse
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.ROLE_USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Required by JPA. */
    protected User() {
    }

    /**
     * Convenience constructor for registration. passwordHash must already be
     * BCrypt-hashed by the caller (AuthService) - this entity never hashes,
     * it only stores.
     */
    public User(String email, String passwordHash, String firstName, String lastName, String baseCurrency) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.baseCurrency = (baseCurrency != null && !baseCurrency.isBlank()) ? baseCurrency : "USD";
        this.role = Role.ROLE_USER;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // --- Getters and setters ---

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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
        // Mirrors the constructor's guard - without this, a blank value passed
        // here fails obscurely at flush time against the NOT NULL constraint
        // instead of failing clearly at the point of misuse.
        this.baseCurrency = (baseCurrency != null && !baseCurrency.isBlank()) ? baseCurrency : "USD";
    }

    public Role getRole() {
        return role;
    }

    // TODO: no DTO binds to this yet (registration always defaults to
    // ROLE_USER). If a future admin-management endpoint sets this from a
    // request body, validate against Role.values() explicitly and never bind
    // it directly from client-controlled JSON - that's a privilege-escalation
    // path (client sends role: "ROLE_ADMIN" in a registration payload).
    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        // Entities are compared by DB identity, not by field values - two
        // transient (unsaved) users are never equal to each other.
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        // Deliberately excludes passwordHash - entities get logged (e.g. in
        // exception stack traces) more often than anyone intends.
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", baseCurrency='" + baseCurrency + '\'' +
                ", role=" + role +
                ", createdAt=" + createdAt +
                '}';
    }
}