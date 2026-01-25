package org.gb.billing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gb.billing.dto.response.*;
import org.gb.billing.entity.User;
import org.gb.billing.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for subscription analytics.
 * Requires ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/v1/admin/analytics")
@Tag(name = "Analytics", description = "Admin analytics for subscription metrics and revenue")
@SecurityRequirement(name = "bearer-jwt")
@PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsController.class);

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/subscriptions/by-plan")
    @Operation(summary = "Get active subscriptions by plan")
    public ResponseEntity<List<SubscriptionCountByPlan>> getSubscriptionCountByPlan(@AuthenticationPrincipal User user) {
        UUID tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.getSubscriptionCountByPlan(tenantId));
    }

    @GetMapping("/subscriptions/by-status")
    @Operation(summary = "Get subscriptions by status")
    public ResponseEntity<List<SubscriptionCountByStatus>> getSubscriptionCountByStatus(@AuthenticationPrincipal User user) {
        UUID tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.getSubscriptionCountByStatus(tenantId));
    }

    @GetMapping("/churn-rate")
    @Operation(summary = "Get churn rate (last 30 days)")
    public ResponseEntity<ChurnRateResponse> getChurnRate(@AuthenticationPrincipal User user) {
        UUID tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.calculateChurnRate(tenantId));
    }

    @GetMapping("/subscription-growth")
    @Operation(summary = "Get subscription growth trends (last 30 days)")
    public ResponseEntity<List<SubscriptionGrowthData>> getSubscriptionGrowth(@AuthenticationPrincipal User user) {
        UUID tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.getSubscriptionGrowth(tenantId));
    }

    @GetMapping("/revenue-summary")
    @Operation(summary = "Get revenue summary (MRR/ARR)")
    public ResponseEntity<RevenueSummaryResponse> getRevenueSummary(@AuthenticationPrincipal User user) {
        UUID tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.getRevenueSummary(tenantId));
    }

    private UUID getTenantId(User user) {
        if (user.getTenantId() == null) {
             // For safety, though users should have tenantId. Admin might be system admin? 
             // Assuming tenant-scoped admin for now.
             throw new IllegalStateException("User does not belong to a tenant");
        }
        return UUID.nameUUIDFromBytes(user.getTenantId().toString().getBytes());
    }
}
