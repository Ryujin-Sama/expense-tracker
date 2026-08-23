package com.expenseTracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Spring Configuration class for password encoding and hashing.
 * 
 * This class provides the PasswordEncoder bean used throughout the application for:
 * - Hashing passwords during user registration
 * - Verifying passwords during user authentication
 * - Encoding new passwords when users change their password
 * 
 * Design Decision - Separate Config Class:
 * This is deliberately split into its own small @Configuration class rather than
 * bundled into a larger SecurityConfig for these reasons:
 * 
 * 1. Dependency Order:
 *    - AuthService (password hashing) needs PasswordEncoder immediately
 *    - Full SecurityConfig (HTTP filter chain, CSRF, CORS, JwtAuthFilter) depends on JwtUtil
 *    - JwtUtil is not built until later in development
 *    - This separation allows PasswordEncoder to be available independently
 * 
 * 2. Single Responsibility:
 *    - Keeps @Configuration classes focused and easy to review
 *    - One class = one concern (password encoding)
 *    - Better than accumulating unrelated beans in one large SecurityConfig
 *    - Easier to test and modify independently
 * 
 * 3. Modularity:
 *    - New developers can focus on password encoding in isolation
 *    - Future enhancements to hashing don't require modifying security filter config
 *    - Easier to review during code reviews
 * 
 * BCrypt Algorithm Details:
 * - Adaptive hashing function that can slow down hashing as computers get faster
 * - Automatically generates and stores salt in the hash output
 * - Default strength is 10 rounds ("cost" parameter)
 * - 10 rounds is current best practice (2026)
 * - One BCrypt operation takes ~100ms on modern hardware (intentional slowness)
 * - Slow hashing prevents brute-force password attack success
 * 
 * Password Encoding Flow:
 * 1. User submits password via registration/password-change endpoint
 * 2. PasswordEncoder.encode(plainPassword) produces hash + salt
 * 3. Hash is stored in User.passwordHash column
 * 4. Plain password is never stored
 * 5. On login: PasswordEncoder.matches(plainPassword, storedHash) verifies
 * 
 * Security Implications:
 * - NEVER compare passwords with .equals() or ==
 * - ALWAYS use passwordEncoder.matches() for verification
 * - NEVER log or display password hashes in user-facing errors
 * - Hash is unique even for same password (due to random salt)
 * - Hash output is deterministic ONLY for matches(), not for display
 * 
 * @see org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
 * @see com.expenseTracker.model.entity.User
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Default strength (10 rounds) - reasonable today; revisit only if
        // profiling ever shows login/register latency dominated by hashing.
        return new BCryptPasswordEncoder();
    }
}