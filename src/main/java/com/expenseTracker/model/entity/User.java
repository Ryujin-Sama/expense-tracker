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