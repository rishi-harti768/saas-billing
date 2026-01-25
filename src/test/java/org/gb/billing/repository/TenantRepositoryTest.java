package org.gb.billing.repository;

import org.gb.billing.entity.Tenant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests for TenantRepository.
 * Tests User Story 2: Tenant management for multi-tenancy.
 */
@DataJpaTest
@DisplayName("TenantRepository Tests")
class TenantRepositoryTest {

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    @DisplayName("Should find tenant by name when exists")
    void testFindByName_TenantExists() {
        // Arrange
        Tenant tenant = new Tenant("Acme Corp");
        tenantRepository.save(tenant);

        // Act
        Optional<Tenant> found = tenantRepository.findByName("Acme Corp");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Corp");
        assertThat(found.get().getActive()).isTrue();
    }

    @Test
    @DisplayName("Should return empty when tenant not found by name")
    void testFindByName_TenantNotFound() {
        // Act
        Optional<Tenant> found = tenantRepository.findByName("Nonexistent Corp");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Should enforce unique name constraint")
    void testSave_DuplicateName() {
        // Arrange
        Tenant tenant1 = new Tenant("Duplicate Corp");
        Tenant tenant2 = new Tenant("Duplicate Corp");
        
        tenantRepository.save(tenant1);
        tenantRepository.flush();

        // Act & Assert
        assertThatThrownBy(() -> {
            tenantRepository.save(tenant2);
            tenantRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Should save tenant with audit fields")
    void testSave_WithAuditFields() {
        // Arrange
        Tenant tenant = new Tenant("Test Corp");

        // Act
        Tenant saved = tenantRepository.save(tenant);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("Test Corp");
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getCreatedDate()).isNotNull();
        assertThat(saved.getLastModifiedDate()).isNotNull();
    }
}
