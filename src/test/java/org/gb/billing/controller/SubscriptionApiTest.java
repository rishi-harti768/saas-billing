package org.gb.billing.controller;

import org.gb.billing.config.SecurityConfig;
import org.gb.billing.config.CorsConfig;
import org.gb.billing.security.WithMockCustomUser;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gb.billing.dto.request.SubscribeRequest;
import org.gb.billing.dto.request.UpgradeRequest;
import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.Subscription;
import org.gb.billing.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SubscriptionController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SubscriptionApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SubscriptionService subscriptionService;

    @MockBean
    private io.github.bucket4j.distributed.proxy.ProxyManager<String> proxyManager;

    @MockBean
    private org.gb.billing.config.RateLimitConfig rateLimitConfig;

    @MockBean
    private org.gb.billing.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.gb.billing.security.CustomUserDetailsService userDetailsService;

    @Test
    @WithMockCustomUser(id = 1L, email = "user1@example.com", tenantId = 1L)
    void shouldCreateSubscription() throws Exception {
        UUID planId = UUID.randomUUID();
        Long userId = 1L;
        Long tenantId = 1L;

        SubscribeRequest request = new SubscribeRequest();
        request.setPlanId(planId);

        BillingPlan plan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        plan.setId(planId);

        Subscription subscription = new Subscription(userId, tenantId, plan);
        subscription.setId(UUID.randomUUID());

        when(subscriptionService.createSubscription(any(), any(), eq(planId))).thenReturn(subscription);

        mockMvc.perform(post("/api/v1/subscriptions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockCustomUser(id = 1L, email = "user1@example.com", tenantId = 1L)
    void shouldGetMySubscription() throws Exception {
        Long userId = 1L;
        Long tenantId = 1L;
        BillingPlan plan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        
        Subscription subscription = new Subscription(userId, tenantId, plan);
        subscription.setId(UUID.randomUUID());

        when(subscriptionService.getMySubscription(any(), any())).thenReturn(Optional.of(subscription));

        mockMvc.perform(get("/api/v1/subscriptions/my-subscription"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockCustomUser(id = 1L, email = "user1@example.com", tenantId = 1L)
    void shouldReturn404WhenNoActiveSubscription() throws Exception {
        when(subscriptionService.getMySubscription(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/subscriptions/my-subscription"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockCustomUser(id = 1L, email = "user1@example.com", tenantId = 1L)
    void shouldGetSubscriptionById() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        Long userId = 1L;
        Long tenantId = 1L;
        BillingPlan plan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        
        Subscription subscription = new Subscription(userId, tenantId, plan);
        subscription.setId(subscriptionId);

        when(subscriptionService.getSubscriptionById(eq(subscriptionId), any())).thenReturn(subscription);

        mockMvc.perform(get("/api/v1/subscriptions/{id}", subscriptionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subscriptionId.toString()));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/subscriptions/my-subscription"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockCustomUser(id = 1L, email = "user1@example.com", tenantId = 1L)
    void shouldReturn400WhenInvalidSubscribeRequest() throws Exception {
        SubscribeRequest request = new SubscribeRequest();

        mockMvc.perform(post("/api/v1/subscriptions")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockCustomUser(id = 1L, email = "user1@example.com", tenantId = 1L)
    void shouldUpgradeSubscription() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        UUID newPlanId = UUID.randomUUID();
        Long tenantId = 1L;
        
        UpgradeRequest request = new UpgradeRequest();
        request.setNewPlanId(newPlanId);

        BillingPlan newPlan = new BillingPlan("Enterprise", "Enterprise tier", new BigDecimal("99.99"), BillingCycle.MONTHLY);
        newPlan.setId(newPlanId);

        Subscription upgradedSubscription = new Subscription(1L, tenantId, newPlan);
        upgradedSubscription.setId(subscriptionId);

        when(subscriptionService.upgradeSubscription(eq(subscriptionId), eq(tenantId), eq(newPlanId)))
                .thenReturn(upgradedSubscription);

        mockMvc.perform(put("/api/v1/subscriptions/{id}/upgrade", subscriptionId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @WithMockCustomUser(id = 1L, email = "user1@example.com", tenantId = 1L)
    void shouldCancelSubscription() throws Exception {
        UUID subscriptionId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/subscriptions/{id}", subscriptionId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
