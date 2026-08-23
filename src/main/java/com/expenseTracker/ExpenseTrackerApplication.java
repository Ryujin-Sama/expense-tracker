package com.expenseTracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Expense Tracker Spring Boot application.
 * 
 * This is the primary bootstrap class that initializes the Spring application context,
 * configures auto-configuration, and starts the embedded Tomcat server.
 * 
 * The {@code @SpringBootApplication} annotation combines three key annotations:
 * - {@code @Configuration}: Marks this class as a Spring configuration class
 * - {@code @EnableAutoConfiguration}: Enables Spring Boot's auto-configuration
 * - {@code @ComponentScan}: Enables component scanning for Spring beans in this package and subpackages
 * 
 * Application Features (Sprint 1):
 * - JWT-based stateless authentication
 * - BCrypt password hashing and encryption
 * - Role-based access control (RBAC) with USER and ADMIN roles
 * - Flyway database schema versioning and migrations
 * - Spring Data JPA for database operations
 * - Spring Security for comprehensive security configuration
 * 
 * Configuration:
 * - Application properties are loaded from {@code application.yml} and environment-specific overrides
 * - Default profile is 'dev' unless overridden via SPRING_PROFILES_ACTIVE environment variable
 * - Database connection, JWT settings, and CORS are configurable via properties
 * 
 * To run the application:
 * {@code mvn spring-boot:run}
 * 
 * @version 0.1.0 (Sprint 1: Foundation and Security)
 * @since 2026-08-23
 */
@SpringBootApplication
public class ExpenseTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenseTrackerApplication.class, args);
    }
}