package org.gb.billing.integration;

import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.Role;
import org.gb.billing.entity.Tenant;
import org.gb.billing.entity.User;
import org.gb.billing.repository.PlanRepository;
import org.gb.billing.repository.TenantRepository;
import org.gb.billing.repository.UserRepository;
import org.gb.billing.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String freeUserToken;
    private String proUserToken;

    @BeforeEach
    void setUp() {
        // Create Tenants
        Tenant tenant1 = new Tenant();
        tenant1.setName("Tenant 1");
        tenant1 = tenantRepository.save(tenant1);

        Tenant tenant2 = new Tenant();
        tenant2.setName("Tenant 2");
        tenant2 = tenantRepository.save(tenant2);

        // Create Free User
        User freeUser = new User();
        freeUser.setEmail("free@example.com");
        freeUser.setPassword(passwordEncoder.encode("password"));
        freeUser.setRole(Role.ROLE_USER);
        freeUser.setTenant(tenant1);
        freeUser.setActive(true);
        freeUser = userRepository.save(freeUser);

        // Create Free Plan
        BillingPlan freePlan = new BillingPlan();
        freePlan.setName("Free");
        freePlan.setPrice(BigDecimal.ZERO);
        freePlan.setBillingCycle(org.gb.billing.entity.BillingCycle.MONTHLY);
        freePlan.setIsActive(true);
        planRepository.save(freePlan);

        freeUserToken = "Bearer " + tokenProvider.generateToken(freeUser.getId(), freeUser.getEmail(), freeUser.getRole().name(), freeUser.getTenantId());

        // Create Pro User
        User proUser = new User();
        proUser.setEmail("pro@example.com");
        proUser.setPassword(passwordEncoder.encode("password"));
        proUser.setRole(Role.ROLE_USER);
        proUser.setTenant(tenant2);
        proUser.setActive(true);
        proUser = userRepository.save(proUser);

        proUserToken = "Bearer " + tokenProvider.generateToken(proUser.getId(), proUser.getEmail(), proUser.getRole().name(), proUser.getTenantId());
    }

    @Test
    void testFreeUserRateLimit() throws Exception {
        // Make 10 successful requests
        for (int i = 1; i <= 10; i++) {
            mockMvc.perform(get("/api/v1/plans")
                    .header("Authorization", freeUserToken))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-RateLimit-Remaining"))
                    .andExpect(header().string("X-RateLimit-Limit", "10"));
        }

        // 11th request should be rate limited
        mockMvc.perform(get("/api/v1/plans/active")
                .header("Authorization", freeUserToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"));
    }

    @Test
    void testProUserHigherLimit() throws Exception {
        // Pro user should be able to make more than 10 requests (limit is 100)
        for (int i = 1; i <= 15; i++) {
            mockMvc.perform(get("/api/v1/plans")
                    .header("Authorization", proUserToken))
                    .andExpect(status().isOk())
                    .andExpect(header().string("X-RateLimit-Limit", "100"));
        }
    }
}
