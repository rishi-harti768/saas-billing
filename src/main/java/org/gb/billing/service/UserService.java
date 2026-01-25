package org.gb.billing.service;

import org.gb.billing.entity.Role;
import org.gb.billing.entity.Tenant;
import org.gb.billing.entity.User;
import org.gb.billing.exception.DuplicateEmailException;
import org.gb.billing.repository.TenantRepository;
import org.gb.billing.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user management operations.
 * Handles user registration with password hashing and tenant association.
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TenantRepository tenantRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user with the specified details.
     * 
     * @param email user's email address
     * @param password user's plain text password (will be hashed)
     * @param role user's role (ROLE_ADMIN or ROLE_USER)
     * @param tenantName tenant name (required for ROLE_USER, must be null for ROLE_ADMIN)
     * @param firstName user's first name (optional)
     * @param lastName user's last name (optional)
     * @return the created user
     * @throws DuplicateEmailException if email already exists
     * @throws IllegalArgumentException if ROLE_USER is provided without tenantName
     */
    @Transactional
    public User registerUser(String email, String password, Role role, String tenantName, String firstName, String lastName) {
        logger.debug("Registering user with email: {} and role: {}", email, role);

        // Validate tenant requirements based on role
        if (role == Role.ROLE_USER && (tenantName == null || tenantName.isBlank())) {
            throw new IllegalArgumentException("Tenant name is required for ROLE_USER");
        }

        // Hash the password
        String hashedPassword = passwordEncoder.encode(password);

        // Create user entity
        User user = new User(email, hashedPassword, role);
        user.setFirstName(firstName);
        user.setLastName(lastName);

        // Handle tenant association for ROLE_USER
        if (role == Role.ROLE_USER) {
            Tenant tenant = tenantRepository.findByName(tenantName)
                    .orElseGet(() -> {
                        logger.info("Creating new tenant: {}", tenantName);
                        Tenant newTenant = new Tenant(tenantName);
                        return tenantRepository.save(newTenant);
                    });
            user.setTenant(tenant);
            logger.debug("Associated user with tenant: {}", tenantName);
        }

        // Save user
        try {
            User savedUser = userRepository.save(user);
            logger.info("Successfully registered user: {} with role: {}", email, role);
            return savedUser;
        } catch (DataIntegrityViolationException ex) {
            logger.warn("Duplicate email registration attempt: {}", email);
            throw new DuplicateEmailException("Email already registered: " + email);
        }
    }
}
