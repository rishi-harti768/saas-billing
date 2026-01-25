package org.gb.billing.service;

import org.gb.billing.entity.Role;
import org.gb.billing.entity.Tenant;
import org.gb.billing.entity.User;
import org.gb.billing.exception.InvalidCredentialsException;
import org.gb.billing.repository.UserRepository;
import org.gb.billing.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthenticationService.
 * Tests User Story 1: Login functionality and JWT generation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationService Tests")
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User testUser;
    private static final String TEST_EMAIL = "admin@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$12$hashedpassword";
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

    @BeforeEach
    void setUp() {
        testUser = new User(TEST_EMAIL, HASHED_PASSWORD, Role.ROLE_ADMIN);
        testUser.setId(1L);
        testUser.setActive(true);
    }

    @Test
    @DisplayName("Should successfully login and return JWT token")
    void testLogin_Success() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, TEST_EMAIL, "ROLE_ADMIN", null)).thenReturn(TEST_TOKEN);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String token = authenticationService.login(TEST_EMAIL, TEST_PASSWORD);

        // Assert
        assertThat(token).isEqualTo(TEST_TOKEN);
        verify(userRepository).findByEmail(TEST_EMAIL);
        verify(passwordEncoder).matches(TEST_PASSWORD, HASHED_PASSWORD);
        verify(jwtTokenProvider).generateToken(1L, TEST_EMAIL, "ROLE_ADMIN", null);
        verify(userRepository).save(argThat(user -> user.getLastLoginDate() != null));
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when email not found")
    void testLogin_EmailNotFound() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.login(TEST_EMAIL, TEST_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verify(userRepository).findByEmail(TEST_EMAIL);
        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException when password is wrong")
    void testLogin_WrongPassword() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.login(TEST_EMAIL, TEST_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Invalid credentials");

        verify(passwordEncoder).matches(TEST_PASSWORD, HASHED_PASSWORD);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("Should throw exception when account is inactive")
    void testLogin_InactiveAccount() {
        // Arrange
        testUser.setActive(false);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> authenticationService.login(TEST_EMAIL, TEST_PASSWORD))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("Account is disabled");

        verifyNoInteractions(passwordEncoder, jwtTokenProvider);
    }

    @Test
    @DisplayName("Should update lastLoginDate on successful login")
    void testLogin_UpdatesLastLoginDate() {
        // Arrange
        LocalDateTime beforeLogin = LocalDateTime.now();
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtTokenProvider.generateToken(anyLong(), anyString(), anyString(), any())).thenReturn(TEST_TOKEN);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authenticationService.login(TEST_EMAIL, TEST_PASSWORD);

        // Assert
        verify(userRepository).save(argThat(user -> {
            LocalDateTime lastLogin = user.getLastLoginDate();
            return lastLogin != null && 
                   (lastLogin.isEqual(beforeLogin) || lastLogin.isAfter(beforeLogin));
        }));
    }

    @Test
    @DisplayName("Should include tenantId in JWT for ROLE_USER")
    void testLogin_IncludesTenantIdForUser() {
        // Arrange
        Tenant tenant = new Tenant("Acme Corp");
        tenant.setId(5L);
        
        User subscriberUser = new User("user@example.com", HASHED_PASSWORD, Role.ROLE_USER);
        subscriberUser.setId(2L);
        subscriberUser.setTenant(tenant);
        subscriberUser.setActive(true);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(subscriberUser));
        when(passwordEncoder.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtTokenProvider.generateToken(2L, "user@example.com", "ROLE_USER", 5L)).thenReturn(TEST_TOKEN);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String token = authenticationService.login("user@example.com", TEST_PASSWORD);

        // Assert
        assertThat(token).isEqualTo(TEST_TOKEN);
        verify(jwtTokenProvider).generateToken(2L, "user@example.com", "ROLE_USER", 5L);
    }

    @Test
    @DisplayName("Should have null tenantId in JWT for ROLE_ADMIN")
    void testLogin_NullTenantIdForAdmin() {
        // Arrange
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, TEST_EMAIL, "ROLE_ADMIN", null)).thenReturn(TEST_TOKEN);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        authenticationService.login(TEST_EMAIL, TEST_PASSWORD);

        // Assert
        verify(jwtTokenProvider).generateToken(1L, TEST_EMAIL, "ROLE_ADMIN", null);
    }
}
