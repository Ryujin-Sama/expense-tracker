package com.expenseTracker.model.entity;

/**
 * Enumeration of authorization roles in the Expense Tracker system.
 * 
 * Role-Based Access Control (RBAC):
 * The system uses two primary roles to control access to features and operations:
 * 
 * Roles:
 * - ROLE_USER: Regular authenticated user
 *   * Can create and manage their own expenses
 *   * Can view their own account information
 *   * Cannot access admin features
 *   * Default role for new registrations
 * 
 * - ROLE_ADMIN: Administrator with elevated privileges
 *   * Can perform all user actions
 *   * Can manage other users' accounts
 *   * Can view system-wide reports
 *   * Can change user roles and permissions
 *   * Assigned manually (not via registration)
 * 
 * Database Storage:
 * - Stored as STRING in database (using @Enumerated(EnumType.STRING))
 * - Database column contains actual enum constant name ("ROLE_USER", "ROLE_ADMIN")
 * - This provides human-readable values in the database
 * - Matches the FDS (Functional Design Specification) naming convention
 * - Java code gets compile-time safety via enum instead of comparing raw strings
 * 
 * Spring Security Integration:
 * - Role names are prefixed with "ROLE_" (Spring Security convention)
 * - GrantedAuthority uses role.name() (e.g., "ROLE_USER")
 * - @PreAuthorize("hasRole('USER')") checks for ROLE_USER in code
 * - Authority comparison is case-sensitive and includes the "ROLE_" prefix
 * 
 * Usage Example:
 * User user = userRepository.findByEmail(email);
 * if (user.getRole() == Role.ROLE_ADMIN) {
 *     // Admin-only operation
 * }
 * 
 * Future Extensions:
 * As the system grows, additional roles may include:
 * - ROLE_MANAGER: Department manager with reporting features
 * - ROLE_AUDITOR: Read-only access for compliance
 * - ROLE_ACCOUNTANT: Access to financial summaries
 * 
 * @see com.expenseTracker.model.entity.User
 * @see com.expenseTracker.security.UserPrincipal
 */
public enum Role {
    ROLE_USER,
    ROLE_ADMIN
}