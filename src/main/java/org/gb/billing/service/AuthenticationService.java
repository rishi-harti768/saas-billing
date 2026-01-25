package org.gb.billing.service;

import org.gb.billing.entity.User;
import org.gb.billing.exception.InvalidCredentialsException;
import org.gb.billing.repository.UserRepository;
import org.gb.billing.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service for authentication operations.
 * Handles user login and JWT token generation.
 */
@Service
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Authenticate a user and generate a JWT token.
     * 
     * @param email user's email address
     * @param password user's plain text password
     * @return JWT token
     * @throws InvalidCredentialsException if email or password is invalid, or account is disabled
     */
    @Transactional
    public String login(String email, String password) {
        logger.debug("Login attempt for email: {}", email);

        // Find user by email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Login failed: user not found for email: {}", email);
                    return new InvalidCredentialsException("Invalid credentials");
                });

        // Check if account is active
        if (!user.getActive()) {
            logger.warn("Login failed: account disabled for email: {}", email);
            throw new InvalidCredentialsException("Account is disabled");
        }

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            logger.warn("Login failed: invalid password for email: {}", email);
            throw new InvalidCredentialsException("Invalid credentials");
        }

        // Update last login date
        user.setLastLoginDate(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT token
        String token = jwtTokenProvider.generateToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getTenantId()
        );

        logger.info("Successful login for user: {} with role: {}", email, user.getRole());
        return token;
    }
}
