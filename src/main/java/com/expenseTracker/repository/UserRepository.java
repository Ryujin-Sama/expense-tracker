package com.expenseTracker.repository;

import com.expenseTracker.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    /**
     * Used for the registration pre-check (Day 4, AuthService) so we can
     * return a clean 409 Conflict instead of letting the unique constraint
     * throw a DataIntegrityViolationException from deep inside Hibernate.
     */
    boolean existsByEmail(String email);
}