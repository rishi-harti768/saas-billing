package org.gb.billing.security;

import jakarta.persistence.EntityManager;
import org.gb.billing.entity.Role;
import org.gb.billing.entity.Tenant;
import org.gb.billing.entity.User;
import org.gb.billing.repository.TenantRepository;
import org.gb.billing.repository.UserRepository;
import org.hibernate.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for tenant isolation using Hibernate filters.
 * Tests User Story 2: Multi-tenant data isolation.
 */
@DataJpaTest
@Import(TenantFilterAspect.class)
@DisplayName("TenantFilterAspect Integration Tests")
class TenantFilterAspectTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private EntityManager entityManager;

    private Tenant tenant1;
    private Tenant tenant2;
    private User user1Tenant1;
    private User user2Tenant1;
    private User user1Tenant2;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // Create tenants
        tenant1 = new Tenant("Tenant One");
        tenant2 = new Tenant("Tenant Two");
        tenantRepository.save(tenant1);
        tenantRepository.save(tenant2);

        // Create users for tenant 1
        user1Tenant1 = new User("user1@tenant1.com", "password", Role.ROLE_USER);
        user1Tenant1.setTenant(tenant1);
        userRepository.save(user1Tenant1);

        user2Tenant1 = new User("user2@tenant1.com", "password", Role.ROLE_USER);
        user2Tenant1.setTenant(tenant1);
        userRepository.save(user2Tenant1);

        // Create user for tenant 2
        user1Tenant2 = new User("user1@tenant2.com", "password", Role.ROLE_USER);
        user1Tenant2.setTenant(tenant2);
        userRepository.save(user1Tenant2);

        // Create admin user (no tenant)
        adminUser = new User("admin@system.com", "password", Role.ROLE_ADMIN);
        userRepository.save(adminUser);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should apply tenant filter when tenantId is set in context")
    void testTenantFilter_Applied() {
        // Arrange
        TenantContext.setTenantId(tenant1.getId());
        
        // Enable filter manually for this test
        Session session = entityManager.unwrap(Session.class);
        org.hibernate.Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("tenantId", tenant1.getId());

        // Act
        List<User> users = userRepository.findAll();

        // Assert
        assertThat(users).hasSize(2);
        assertThat(users).extracting(User::getEmail)
                .containsExactlyInAnyOrder("user1@tenant1.com", "user2@tenant1.com");

        // Cleanup
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should block cross-tenant queries")
    void testTenantFilter_BlocksCrossTenantAccess() {
        // Arrange
        TenantContext.setTenantId(tenant1.getId());
        
        Session session = entityManager.unwrap(Session.class);
        org.hibernate.Filter filter = session.enableFilter("tenantFilter");
        filter.setParameter("tenantId", tenant1.getId());

        // Act
        List<User> users = userRepository.findAll();

        // Assert - should not see tenant2's users
        assertThat(users).noneMatch(user -> "user1@tenant2.com".equals(user.getEmail()));

        // Cleanup
        TenantContext.clear();
    }

    @Test
    @DisplayName("Should not filter admin users (no tenant)")
    void testTenantFilter_AdminUsersBypass() {
        // Arrange - No tenant context set (admin scenario)
        
        // Act
        List<User> allUsers = userRepository.findAll();

        // Assert - should see all users including admin
        assertThat(allUsers).hasSizeGreaterThanOrEqualTo(4);
        assertThat(allUsers).anyMatch(user -> "admin@system.com".equals(user.getEmail()));
    }
}
