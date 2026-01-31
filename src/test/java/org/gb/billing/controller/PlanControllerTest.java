package org.gb.billing.controller;

import org.gb.billing.config.SecurityConfig;
import org.gb.billing.config.CorsConfig;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gb.billing.dto.request.CreatePlanRequest;
import org.gb.billing.dto.request.UpdatePlanRequest;
import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.service.PlanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for PlanController.
 * Uses MockMvc for testing HTTP endpoints with security context.
 */
@WebMvcTest(PlanController.class)
@Import({SecurityConfig.class, CorsConfig.class})
@AutoConfigureMockMvc
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PlanService planService;

    @MockBean
    private org.gb.billing.service.SubscriptionService subscriptionService;

    @MockBean
    private io.github.bucket4j.distributed.proxy.ProxyManager<String> proxyManager;

    @MockBean
    private org.gb.billing.config.RateLimitConfig rateLimitConfig;

    @MockBean
    private org.gb.billing.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private org.gb.billing.security.CustomUserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreatePlan() throws Exception {
        // Given
        CreatePlanRequest request = new CreatePlanRequest();
        request.setName("Pro");
        request.setDescription("Professional tier");
        request.setPrice(new BigDecimal("29.99"));
        request.setBillingCycle("MONTHLY");

        BillingPlan plan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        plan.setId(UUID.randomUUID());

        when(planService.createPlan(any())).thenReturn(plan);

        // When/Then
        mockMvc.perform(post("/api/v1/plans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Pro"))
                .andExpect(jsonPath("$.price").value(29.99));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetAllPlans() throws Exception {
        // Given
        BillingPlan plan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        when(planService.getAllPlans()).thenReturn(List.of(plan));

        // When/Then
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Pro"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetPlanById() throws Exception {
        // Given
        UUID planId = UUID.randomUUID();
        BillingPlan plan = new BillingPlan("Pro", "Professional tier", new BigDecimal("29.99"), BillingCycle.MONTHLY);
        plan.setId(planId);

        when(planService.getPlanById(planId)).thenReturn(plan);

        // When/Then
        mockMvc.perform(get("/api/v1/plans/{id}", planId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pro"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldUpdatePlan() throws Exception {
        // Given
        UUID planId = UUID.randomUUID();
        UpdatePlanRequest request = new UpdatePlanRequest();
        request.setDescription("Updated description");
        request.setPrice(new BigDecimal("39.99"));

        BillingPlan plan = new BillingPlan("Pro", "Updated description", new BigDecimal("39.99"), BillingCycle.MONTHLY);
        plan.setId(planId);

        when(planService.updatePlan(eq(planId), any())).thenReturn(plan);

        // When/Then
        mockMvc.perform(put("/api/v1/plans/{id}", planId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(39.99));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldDeletePlan() throws Exception {
        // Given
        UUID planId = UUID.randomUUID();

        // When/Then
        mockMvc.perform(delete("/api/v1/plans/{id}", planId)
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // When/Then
        mockMvc.perform(get("/api/v1/plans"))
                .andExpect(status().isForbidden()); // Spring Security default for anonymous REST
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403WhenNotAdmin() throws Exception {
        // Given
        CreatePlanRequest request = new CreatePlanRequest();
        request.setName("Pro Plan");
        request.setDescription("Professional tier description");
        request.setPrice(new BigDecimal("29.99"));
        request.setBillingCycle("MONTHLY");

        String jsonRequest = "{\"name\":\"Pro Plan\",\"description\":\"Professional tier description\",\"price\":29.99,\"billingCycle\":\"MONTHLY\"}";

        // When/Then
        mockMvc.perform(post("/api/v1/plans")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
