package org.gb.billing.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.*;

/**
 * Security tests for BCrypt password handling.
 * Tests User Story 3: Secure password management and BCrypt implementation.
 */
@DisplayName("Password Security Tests")
class PasswordSecurityTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Test
    @DisplayName("Should never store password in plain text - verify BCrypt hash format")
    void testPassword_NeverPlainText() {
        // Arrange
        String plainPassword = "mySecurePassword123";

        // Act
        String hashedPassword = passwordEncoder.encode(plainPassword);

        // Assert
        assertThat(hashedPassword).isNotEqualTo(plainPassword);
        assertThat(hashedPassword).startsWith("$2a$"); // BCrypt format
        assertThat(hashedPassword).hasSize(60); // BCrypt hash is always 60 characters
    }

    @Test
    @DisplayName("Should use BCrypt work factor 12")
    void testPassword_WorkFactor12() {
        // Arrange
        String plainPassword = "testPassword";

        // Act
        String hashedPassword = passwordEncoder.encode(plainPassword);

        // Assert
        assertThat(hashedPassword).startsWith("$2a$12$"); // Work factor 12
    }

    @Test
    @DisplayName("Should verify password successfully with correct password")
    void testPassword_VerificationSuccess() {
        // Arrange
        String plainPassword = "correctPassword";
        String hashedPassword = passwordEncoder.encode(plainPassword);

        // Act
        boolean matches = passwordEncoder.matches(plainPassword, hashedPassword);

        // Assert
        assertThat(matches).isTrue();
    }

    @Test
    @DisplayName("Should fail verification with incorrect password")
    void testPassword_VerificationFailure() {
        // Arrange
        String plainPassword = "correctPassword";
        String wrongPassword = "wrongPassword";
        String hashedPassword = passwordEncoder.encode(plainPassword);

        // Act
        boolean matches = passwordEncoder.matches(wrongPassword, hashedPassword);

        // Assert
        assertThat(matches).isFalse();
    }

    @Test
    @DisplayName("Should be resistant to timing attacks - verification time should be consistent")
    void testPassword_TimingAttackResistance() {
        // Arrange
        String plainPassword = "password123";
        String hashedPassword = passwordEncoder.encode(plainPassword);

        // Act - Measure time for correct password
        long startCorrect = System.nanoTime();
        passwordEncoder.matches(plainPassword, hashedPassword);
        long timeCorrect = System.nanoTime() - startCorrect;

        // Act - Measure time for wrong password
        long startWrong = System.nanoTime();
        passwordEncoder.matches("wrongPassword", hashedPassword);
        long timeWrong = System.nanoTime() - startWrong;

        // Assert - Times should be similar (within 50% variance)
        // BCrypt is designed to take consistent time regardless of password correctness
        double ratio = (double) Math.max(timeCorrect, timeWrong) / Math.min(timeCorrect, timeWrong);
        assertThat(ratio).isLessThan(1.5); // Allow 50% variance for system fluctuations
    }

    @Test
    @DisplayName("Should generate different hashes for same password (salt)")
    void testPassword_UniqueSalts() {
        // Arrange
        String plainPassword = "samePassword";

        // Act
        String hash1 = passwordEncoder.encode(plainPassword);
        String hash2 = passwordEncoder.encode(plainPassword);

        // Assert
        assertThat(hash1).isNotEqualTo(hash2); // Different salts produce different hashes
        assertThat(passwordEncoder.matches(plainPassword, hash1)).isTrue();
        assertThat(passwordEncoder.matches(plainPassword, hash2)).isTrue();
    }
}
