package com.expenseTracker.security;

import com.expenseTracker.model.entity.User;
import com.expenseTracker.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService implementation.
 * 
 * This service bridges the gap between the User entity and Spring Security's
 * authentication mechanism. It loads user information from the database and
 * converts it to Spring Security's UserDetails interface.
 * 
 * Responsibilities:
 * - Load user by email (username) from database
 * - Convert User entity to UserPrincipal (Spring Security UserDetails)
 * - Handle user not found scenarios
 * - Extract authorities (roles) for authorization checks
 * 
 * Spring Security Integration:
 * UserDetailsService is a core Spring Security interface used by:
 * - DaoAuthenticationProvider (password-based authentication)
 * - JwtAuthFilter (token-based authentication)
 * - Access control filters
 * - Any component checking user permissions
 * 
 * Why Implement UserDetailsService?
 * Spring Security needs a way to load user information from your datastore.
 * This interface is the contract for that. Implementing it allows Spring Security
 * to authenticate users against your User entity in the database.
 * 
 * Critical Security Note - User Enumeration Prevention:
 * 
 * *** IMPORTANT: This service throws UsernameNotFoundException when user is not found.
 * *** HOWEVER, this exception MUST NOT surface to the client as a distinct error
 * *** from "wrong password." Read carefully:
 * 
 * User Enumeration Attack:
 * - Attacker tests different email addresses
 * - Server returns different error messages for each:
 *   * "No user registered with email: john@example.com" → Email doesn't exist
 *   * "Wrong password" → Email exists but wrong password
 * - Attacker uses this to build a list of valid registered emails
 * - This information can be used for targeted attacks, phishing, etc.
 * 
 * OWASP Top 10 Requirement:
 * The FDS specifically requires preventing user enumeration attacks.
 * Both "no such user" and "wrong password" MUST return the SAME error:
 * "Invalid email or password" (generic, no specifics)
 * 
 * Implementation Flow (Must be coordinated with AuthService + GlobalExceptionHandler):
 * 1. AuthService calls authenticate(email, password)
 * 2. DaoAuthenticationProvider calls loadUserByUsername(email)
 * 3. If user not found → UsernameNotFoundException thrown
 * 4. If user found → UserPrincipal returned to authentication provider
 * 5. Provider checks password against stored hash
 * 6. If password wrong → BadCredentialsException thrown
 * 7. GlobalExceptionHandler MUST catch BOTH exceptions:
 *    - UsernameNotFoundException → 401 "Invalid email or password"
 *    - BadCredentialsException → 401 "Invalid email or password"
 * 8. Client receives generic error (cannot distinguish what went wrong)
 * 
 * Why This is Hard to Get Wrong:
 * If you catch BadCredentialsException but not UsernameNotFoundException,
 * the latter will leak different stack traces or error messages to the client.
 * Always test with both valid and invalid emails to verify the error handling.
 * 
 * Method: loadUserByUsername
 * - Parameter name says "username" but we use email (email = username here)
 * - Spring Security method naming convention (historical reasons)
 * - Could be renamed in future, but using standard interface method
 * - Load logic:
 *   1. Query database for user by email
 *   2. If not found, throw UsernameNotFoundException (required by interface)
 *   3. If found, convert to UserPrincipal (Spring Security UserDetails)
 *   4. Return UserPrincipal with authorities/roles
 * 
 * Transaction Context:
 * - @Transactional(readOnly=true) hints to the database this is read-only
 * - Improves performance (database can optimize read-only transactions)
 * - Prevents accidental writes (database enforces read-only at transaction level)
 * - UserRepository query is lazy (only executes when needed)
 * 
 * @see org.springframework.security.core.userdetails.UserDetailsService
 * @see org.springframework.security.core.userdetails.UserDetails
 * @see com.expenseTracker.security.UserPrincipal
 * @see com.expenseTracker.model.entity.User
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