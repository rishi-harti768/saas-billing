package org.gb.billing.controller;

import org.gb.billing.dto.ChurnStats;
import org.gb.billing.dto.MrrStats;
import org.gb.billing.dto.RevenueStats;
import org.gb.billing.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getMrrStats_AsAdmin_ShouldReturn200() throws Exception {
        MrrStats stats = new MrrStats(BigDecimal.ONE, Collections.emptyList());
        given(analyticsService.getMrrStats()).willReturn(stats);

        mockMvc.perform(get("/api/v1/analytics/mrr")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getMrrStats_AsUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/mrr")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getChurnStats_AsAdmin_ShouldReturn200() throws Exception {
        ChurnStats stats = new ChurnStats(100, 5, 5.0);
        given(analyticsService.getChurnStats()).willReturn(stats);

        mockMvc.perform(get("/api/v1/analytics/churn")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getChurnStats_AsUser_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/churn")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}
