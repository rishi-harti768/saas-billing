package org.gb.billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gb.billing.dto.response.*;
import org.gb.billing.entity.User;
import org.gb.billing.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AnalyticsController.class)
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private org.gb.billing.security.CustomUserDetailsService userDetailsService;

    @MockBean
    private org.gb.billing.security.JwtTokenProvider jwtTokenProvider;

    @Test
    void shouldGetSubscriptionCountByPlan() throws Exception {
        UUID planId = UUID.randomUUID();
        SubscriptionCountByPlan stats = new SubscriptionCountByPlan(planId, "Pro", 10L);
        when(analyticsService.getSubscriptionCountByPlan(any())).thenReturn(Collections.singletonList(stats));

        mockMvc.perform(get("/api/v1/admin/analytics/subscriptions/by-plan")
                .with(authentication(getAdminAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].planName").value("Pro"))
                .andExpect(jsonPath("$[0].activeCount").value(10));
    }

    @Test
    void shouldGetChurnRate() throws Exception {
        ChurnRateResponse churn = new ChurnRateResponse(5.0, 5L, 100L, "LAST_30_DAYS");
        when(analyticsService.calculateChurnRate(any())).thenReturn(churn);

        mockMvc.perform(get("/api/v1/admin/analytics/churn-rate")
                .with(authentication(getAdminAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.churnRatePercentage").value(5.0));
    }

    @Test
    void shouldGetRevenueSummary() throws Exception {
        RevenueSummaryResponse revenue = new RevenueSummaryResponse(
                new BigDecimal("1000.00"), 
                new BigDecimal("12000.00"), 
                Collections.emptyList()
        );
        when(analyticsService.getRevenueSummary(any())).thenReturn(revenue);

        mockMvc.perform(get("/api/v1/admin/analytics/revenue-summary")
                .with(authentication(getAdminAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRecurringRevenue").value(1000.0));
    }

    @Test
    void shouldDenyAccessToNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/revenue-summary")
                .with(authentication(getUserAuthentication())))
                .andExpect(status().isForbidden());
    }

    private org.springframework.security.core.Authentication getAdminAuthentication() {
        org.gb.billing.entity.Tenant tenant = new org.gb.billing.entity.Tenant();
        tenant.setId(1L);
        
        org.gb.billing.entity.User user = new org.gb.billing.entity.User();
        user.setId(1L);
        user.setEmail("admin@example.com");
        user.setRole(org.gb.billing.entity.Role.ROLE_ADMIN);
        user.setTenant(tenant);

        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private org.springframework.security.core.Authentication getUserAuthentication() {
        org.gb.billing.entity.Tenant tenant = new org.gb.billing.entity.Tenant();
        tenant.setId(2L);
        
        org.gb.billing.entity.User user = new org.gb.billing.entity.User();
        user.setId(2L);
        user.setEmail("user@example.com");
        user.setRole(org.gb.billing.entity.Role.ROLE_USER);
        user.setTenant(tenant);

        return new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                user, null, java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );
    }
