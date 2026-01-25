package org.gb.billing.repository;

import org.gb.billing.entity.Role;
import org.gb.billing.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests for UserRepository.
 * Tests User Story 1: Database operations for user management.
 */
@DataJpaTest
@DisplayName("UserRepository Tests")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Should find user by email when exists")
    void testFindByEmail_UserExists() {
        // Arrange
        User user = new User("test@example.com", "hashedPassword", Role.ROLE_ADMIN);
        userRepository.save(user);

        // Act
        Optional<User> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    @Test
    @DisplayName("Should return empty when user not found by email")
    void testFindByEmail_UserNotFound() {
        // Act
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should enforce unique email constraint")
    void testSave_DuplicateEmail() {
        // Arrange
        User user1 = new User("duplicate@example.com", "password1", Role.ROLE_ADMIN);
        User user2 = new User("duplicate@example.com", "password2", Role.ROLE_ADMIN);
        
        userRepository.save(user1);
        userRepository.flush();

        // Act & Assert
        assertThatThrownBy(() -> {
            userRepository.save(user2);
            userRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should increment version on update (optimistic locking)")
    void testOptimisticLocking_VersionIncrement() {
        // Arrange
        User user = new User("version@example.com", "password", Role.ROLE_ADMIN);
        user.setFirstName("John");
        User saved = userRepository.save(user);
        Long initialVersion = saved.getVersion();

        // Act
        saved.setFirstName("Jane");
        User updated = userRepository.save(saved);

        // Assert
        assertThat(updated.getVersion()).isGreaterThan(initialVersion);
    }

    @Test
    @DisplayName("Should save user with all fields correctly")
    void testSave_AllFields() {
        // Arrange
        User user = new User("complete@example.com", "hashedPassword", Role.ROLE_ADMIN);
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setActive(true);

        // Act
        User saved = userRepository.save(user);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getEmail()).isEqualTo("complete@example.com");
        assertThat(saved.getPassword()).isEqualTo("hashedPassword");
        assertThat(saved.getRole()).isEqualTo(Role.ROLE_ADMIN);
        assertThat(saved.getFirstName()).isEqualTo("John");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getLastModifiedDate()).isNotNull();
        assertThat(saved.getVersion()).isEqualTo(0L);
    }
}
