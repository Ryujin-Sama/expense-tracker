package com.expenseTracker.repository;

import com.expenseTracker.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data JPA Repository for User entity.
 * 
 * This interface provides database access methods for User entities. Spring Data JPA
 * automatically implements these methods using the method signatures and annotations.
 * 
 * Inherited Methods (from JpaRepository<User, Long>):
 * - save(User user): Persist or update a user
 * - saveAll(Iterable<User> users): Persist multiple users
 * - findById(Long id): Find user by primary key
 * - findAll(): Retrieve all users (use with caution on large tables)
 * - delete(User user): Delete a single user
 * - deleteById(Long id): Delete by primary key
 * - count(): Total number of users
 * - exists(Long id): Check if user exists
 * - flush(): Force pending changes to database
 * - saveAndFlush(User user): Save and immediately flush
 * 
 * Custom Query Methods:
 * 
 * 1. findByEmail(String email)
 *    Purpose: Find user by their email address (username for login)
 *    Returns: Optional<User> (empty if not found)
 *    Usage: AuthService.authenticate() uses this for login
 *    Note: Email is unique constraint, so at most one user per email
 *    Database Query: WHERE email = ? (uses index on email column)
 * 
 * 2. existsByEmail(String email)
 *    Purpose: Check if email is already registered without loading full user
 *    Returns: boolean (true if exists, false otherwise)
 *    Usage: Registration pre-check in AuthService
 *    Benefit: Avoids unique constraint exception from database
 *    Returns clean 409 Conflict error instead of DataIntegrityViolationException
 *    Performance: More efficient than findByEmail().isPresent()
 *    Database Query: WHERE email = ? LIMIT 1 (SELECT COUNT optimized)
 * 
 * Query Method Naming Convention:
 * Spring Data JPA interprets method names to generate queries:
 * - findBy<Field>: Find by column value
 * - countBy<Field>: Count matching records
 * - existsBy<Field>: Check existence
 * - deleteBy<Field>: Delete matching records
 * - And<Field>, Or<Field>: Boolean logic for complex queries
 * 
 * Relationship to User Entity:
 * - User has one-to-many relationship with RefreshToken
 * - User.email is UNIQUE constraint in database
 * - User.id is auto-generated IDENTITY primary key
 * - User.createdAt is set automatically by @PrePersist hook
 * 
 * Transaction Management:
 * - Methods are transactional by default in Spring Data JPA
 * - Read operations (find*, exists*) are read-only by default
 * - Write operations (save, delete) require write access
 * - Use @Transactional(readOnly=true) on service methods for performance hints
 * 
 * Error Handling:
 * - findByEmail() returns empty Optional if not found (no exception)
 * - saveAndFlush() throws DataIntegrityViolationException on unique constraint violation
 * - This is intentional - let registration flow validate uniqueness via existsByEmail first
 * 
 * @see com.expenseTracker.model.entity.User
 * @see com.expenseTracker.security.CustomUserDetailsService
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by their email address.
     * 
     * @param email the email to search for
     * @return Optional containing the User if found, empty Optional otherwise
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if a user with the given email exists.
     * 
     * Used for the registration pre-check so we can return a clean 409 Conflict
     * instead of letting the unique constraint throw a DataIntegrityViolationException
     * from deep inside Hibernate. This is more user-friendly error handling.
     * 
     * @param email the email to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}