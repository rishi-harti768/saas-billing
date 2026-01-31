package org.gb.billing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.gb.billing.dto.ChurnStats;
import org.gb.billing.dto.MrrStats;
import org.gb.billing.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Endpoints for Admin Analytics Dashboard")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/mrr")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Monthly Recurring Revenue", description = "Returns total MRR and monthly breakdown")
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @ApiResponse(responseCode = "403", description = "Access denied (Admin only)")
    public MrrStats getMrrStats() {
        return analyticsService.getMrrStats();
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/subscriptions/by-plan")
    @Operation(summary = "Get active subscriptions by plan")
    public ResponseEntity<List<SubscriptionCountByPlan>> getSubscriptionCountByPlan(@AuthenticationPrincipal User user) {
        Long tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.getSubscriptionCountByPlan(tenantId));
    }

    @GetMapping("/subscriptions/by-status")
    @Operation(summary = "Get subscriptions by status")
    public ResponseEntity<List<SubscriptionCountByStatus>> getSubscriptionCountByStatus(@AuthenticationPrincipal User user) {
        Long tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.getSubscriptionCountByStatus(tenantId));
    }

    @GetMapping("/churn")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Churn Statistics", description = "Returns total subscribers, cancelled subscribers, and churn rate")
    @ApiResponse(responseCode = "200", description = "Successful retrieval")
    @ApiResponse(responseCode = "403", description = "Access denied (Admin only)")
    public ChurnStats getChurnStats() {
        return analyticsService.getChurnStats();
    @GetMapping("/churn-rate")
    @Operation(summary = "Get churn rate (last 30 days)")
    public ResponseEntity<ChurnRateResponse> getChurnRate(@AuthenticationPrincipal User user) {
        Long tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.calculateChurnRate(tenantId));
    }

    @GetMapping("/subscription-growth")
    @Operation(summary = "Get subscription growth trends (last 30 days)")
    public ResponseEntity<List<SubscriptionGrowthData>> getSubscriptionGrowth(@AuthenticationPrincipal User user) {
        Long tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.getSubscriptionGrowth(tenantId));
    }

    @GetMapping("/revenue-summary")
    @Operation(summary = "Get revenue summary (MRR/ARR)")
    public ResponseEntity<RevenueSummaryResponse> getRevenueSummary(@AuthenticationPrincipal User user) {
        Long tenantId = getTenantId(user);
        return ResponseEntity.ok(analyticsService.getRevenueSummary(tenantId));
    }

    private Long getTenantId(User user) {
        if (user.getTenantId() == null) {
             // For safety, though users should have tenantId. Admin might be system admin? 
             // Assuming tenant-scoped admin for now.
             throw new IllegalStateException("User does not belong to a tenant");
        }
        return user.getTenantId();
    }
}
