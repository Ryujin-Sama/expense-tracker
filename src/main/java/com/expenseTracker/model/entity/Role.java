package com.expenseTracker.model.entity;

/**
 * RBAC roles. Stored as its String name (see @Enumerated(EnumType.STRING) on
 * User.role) so the DB column stays human-readable and matches the FDS's
 * "RBAC identifier" convention, while the Java side gets compile-time safety
 * instead of comparing raw strings everywhere.
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}