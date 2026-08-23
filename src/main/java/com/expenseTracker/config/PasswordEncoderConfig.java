package com.expenseTracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Split into its own small config class rather than bundled into SecurityConfig
 * because SecurityConfig (the HttpSecurity filter chain: CSRF, CORS, stateless
 * session, JwtAuthFilter) depends on JwtUtil and doesn't get built until Day 4 -
 * but AuthService needs a PasswordEncoder well before that. Keeping single-
 * purpose @Configuration classes is also just easier to review than one large
 * SecurityConfig accumulating unrelated beans over the sprint.
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