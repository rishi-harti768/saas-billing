package org.gb.billing.security;

import org.gb.billing.config.JwtProperties;
import org.gb.billing.exception.ExpiredTokenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtTokenProvider.
 * Tests User Story 1: JWT token generation, validation, and claim extraction.
 */
@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private JwtProperties jwtProperties;

    private static final String TEST_SECRET = "ThisIsATestSecretKeyForJWTTokenGenerationAndValidation123456";
    private static final Long TEST_EXPIRATION = 3600000L; // 1 hour
    private static final Long USER_ID = 1L;
    private static final String EMAIL = "admin@example.com";
    private static final String ROLE = "ROLE_ADMIN";

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(TEST_SECRET);
        jwtProperties.setExpiration(TEST_EXPIRATION);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    @DisplayName("Should generate valid JWT token with correct claims")
    void testGenerateToken_WithCorrectClaims() {
        // Act
        String token = jwtTokenProvider.generateToken(USER_ID, EMAIL, ROLE, null);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature

        // Verify claims can be extracted
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(USER_ID);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(EMAIL);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(ROLE);
        assertThat(jwtTokenProvider.getTenantIdFromToken(token)).isNull();
    }

    @Test
    @DisplayName("Should generate token with tenantId for ROLE_USER")
    void testGenerateToken_WithTenantId() {
        // Arrange
        Long tenantId = 5L;
        String userEmail = "user@example.com";
        String userRole = "ROLE_USER";

        // Act
        String token = jwtTokenProvider.generateToken(USER_ID, userEmail, userRole, tenantId);

        // Assert
        assertThat(jwtTokenProvider.getTenantIdFromToken(token)).isEqualTo(tenantId);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo(userEmail);
        assertThat(jwtTokenProvider.getRoleFromToken(token)).isEqualTo(userRole);
    }

    @Test
    @DisplayName("Should validate valid token successfully")
    void testValidateToken_ValidToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(USER_ID, EMAIL, ROLE, null);

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject malformed token")
    void testValidateToken_MalformedToken() {
        // Arrange
        String malformedToken = "this.is.not.a.valid.jwt.token";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(malformedToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void testValidateToken_InvalidSignature() {
        // Arrange
        String token = jwtTokenProvider.generateToken(USER_ID, EMAIL, ROLE, null);
        // Tamper with the token by changing the last character
        String tamperedToken = token.substring(0, token.length() - 1) + "X";

        // Act
        boolean isValid = jwtTokenProvider.validateToken(tamperedToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should throw ExpiredTokenException for expired token")
    void testValidateToken_ExpiredToken() {
        // Arrange - Create token with very short expiration
        JwtProperties shortExpirationProps = new JwtProperties();
        shortExpirationProps.setSecret(TEST_SECRET);
        shortExpirationProps.setExpiration(1L); // 1 millisecond
        JwtTokenProvider shortExpirationProvider = new JwtTokenProvider(shortExpirationProps);
        
        String token = shortExpirationProvider.generateToken(USER_ID, EMAIL, ROLE, null);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act & Assert
        assertThatThrownBy(() -> jwtTokenProvider.validateToken(token))
                .isInstanceOf(ExpiredTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("Should reject empty token")
    void testValidateToken_EmptyToken() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Should extract userId from token correctly")
    void testGetUserIdFromToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(USER_ID, EMAIL, ROLE, null);

        // Act
        Long extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // Assert
        assertThat(extractedUserId).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Should extract email from token correctly")
    void testGetEmailFromToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(USER_ID, EMAIL, ROLE, null);

        // Act
        String extractedEmail = jwtTokenProvider.getEmailFromToken(token);

        // Assert
        assertThat(extractedEmail).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Should extract role from token correctly")
    void testGetRoleFromToken() {
        // Arrange
        String token = jwtTokenProvider.generateToken(USER_ID, EMAIL, ROLE, null);

        // Act
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);

        // Assert
        assertThat(extractedRole).isEqualTo(ROLE);
    }

    @Test
    @DisplayName("Should extract tenantId from token correctly")
    void testGetTenantIdFromToken() {
        // Arrange
        Long tenantId = 10L;
        String token = jwtTokenProvider.generateToken(USER_ID, EMAIL, "ROLE_USER", tenantId);

        // Act
        Long extractedTenantId = jwtTokenProvider.getTenantIdFromToken(token);

        // Assert
        assertThat(extractedTenantId).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should return null tenantId for admin token")
    void testGetTenantIdFromToken_NullForAdmin() {
        // Arrange
        String token = jwtTokenProvider.generateToken(USER_ID, EMAIL, ROLE, null);

        // Act
        Long extractedTenantId = jwtTokenProvider.getTenantIdFromToken(token);

        // Assert
        assertThat(extractedTenantId).isNull();
    }
}
