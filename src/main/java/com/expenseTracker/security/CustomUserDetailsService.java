package com.expenseTracker.security;

import com.expenseTracker.model.entity.User;
import com.expenseTracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IMPORTANT for whoever wires this into AuthService/GlobalExceptionHandler
 * (Day 4/6): UsernameNotFoundException below MUST NOT surface to the client
 * as a distinct error from "wrong password." Returning a different message
 * for "no such user" vs. "bad password" is a user-enumeration oracle - an
 * attacker can use it to build a list of registered emails, which is exactly
 * what the FDS's OWASP Top 10 requirement is meant to prevent. Both cases
 * should collapse to one generic "Invalid email or password" 401.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user registered with email: " + email));
        return UserPrincipal.fromUser(user);
    }
}