package org.gb.billing.service;

import org.gb.billing.entity.Role;
import org.gb.billing.entity.Tenant;
import org.gb.billing.entity.User;
import org.gb.billing.exception.DuplicateEmailException;
import org.gb.billing.repository.TenantRepository;
import org.gb.billing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Tests User Story 1: Admin registration functionality.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static final String TEST_EMAIL = "admin@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$12$hashedpassword";

    @BeforeEach
    void setUp() {
        // Common setup if needed
    }

    @Test
    @DisplayName("Should successfully register admin user")
    void testRegisterAdminUser_Success() {
        // Arrange
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        // Act
        User result = userService.registerUser(TEST_EMAIL, TEST_PASSWORD, Role.ROLE_ADMIN, null, "John", "Doe");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(TEST_EMAIL);
        assertThat(result.getPassword()).isEqualTo(HASHED_PASSWORD);
        assertThat(result.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(result.getTenantId()).isNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getLastName()).isEqualTo("Doe");

        verify(passwordEncoder).encode(TEST_PASSWORD);
        verify(userRepository).save(any(User.class));
        verifyNoInteractions(tenantRepository);
    }

    @Test
    @DisplayName("Should throw DuplicateEmailException when email already exists")
    void testRegisterUser_DuplicateEmail() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate email"));

        // Act & Assert
        assertThatThrownBy(() -> 
                userService.registerUser(TEST_EMAIL, TEST_PASSWORD, Role.ROLE_ADMIN, null, null, null))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("Should hash password with BCrypt before saving")
    void testRegisterUser_PasswordIsHashed() {
        // Arrange
        when(passwordEncoder.encode(TEST_PASSWORD)).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.registerUser(TEST_EMAIL, TEST_PASSWORD, Role.ROLE_ADMIN, null, null, null);

        // Assert
        assertThat(result.getPassword()).isEqualTo(HASHED_PASSWORD);
        assertThat(result.getPassword()).isNotEqualTo(TEST_PASSWORD);
        verify(passwordEncoder).encode(TEST_PASSWORD);
    }

    @Test
    @DisplayName("Should validate ROLE_ADMIN has no tenantName required")
    void testRegisterAdmin_NoTenantRequired() {
        // Arrange
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.registerUser(TEST_EMAIL, TEST_PASSWORD, Role.ROLE_ADMIN, null, null, null);

        // Assert
        assertThat(result.getTenant()).isNull();
        verifyNoInteractions(tenantRepository);
    }

    @Test
    @DisplayName("Should throw exception when ROLE_USER is registered without tenantName")
    void testRegisterUser_NoTenantName() {
        // Act & Assert
        assertThatThrownBy(() -> 
                userService.registerUser(TEST_EMAIL, TEST_PASSWORD, Role.ROLE_USER, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant name is required for ROLE_USER");
    }

    @Test
    @DisplayName("Should create new tenant when registering ROLE_USER with new tenant")
    void testRegisterUser_CreateNewTenant() {
        // Arrange
        String tenantName = "Acme Corp";
        when(tenantRepository.findByName(tenantName)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(1L);
            return tenant;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.registerUser(TEST_EMAIL, TEST_PASSWORD, Role.ROLE_USER, tenantName, null, null);

        // Assert
        assertThat(result.getTenant()).isNotNull();
        assertThat(result.getTenant().getName()).isEqualTo(tenantName);
        verify(tenantRepository).findByName(tenantName);
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    @DisplayName("Should use existing tenant when registering ROLE_USER with existing tenant")
    void testRegisterUser_UseExistingTenant() {
        // Arrange
        String tenantName = "Acme Corp";
        Tenant existingTenant = new Tenant(tenantName);
        existingTenant.setId(1L);
        
        when(tenantRepository.findByName(tenantName)).thenReturn(Optional.of(existingTenant));
        when(passwordEncoder.encode(anyString())).thenReturn(HASHED_PASSWORD);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        User result = userService.registerUser(TEST_EMAIL, TEST_PASSWORD, Role.ROLE_USER, tenantName, null, null);

        // Assert
        assertThat(result.getTenant()).isEqualTo(existingTenant);
        verify(tenantRepository).findByName(tenantName);
        verify(tenantRepository, never()).save(any(Tenant.class));
    }
}
