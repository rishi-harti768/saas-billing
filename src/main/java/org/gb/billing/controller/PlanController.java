package org.gb.billing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.gb.billing.dto.request.CreatePlanRequest;
import org.gb.billing.dto.request.FeatureLimitRequest;
import org.gb.billing.dto.request.UpdatePlanRequest;
import org.gb.billing.dto.response.ErrorResponse;
import org.gb.billing.dto.response.PlanResponse;
import org.gb.billing.entity.BillingCycle;
import org.gb.billing.entity.BillingPlan;
import org.gb.billing.entity.FeatureLimit;
import org.gb.billing.service.PlanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for billing plan management.
 * 
 * Endpoints:
 * - POST   /api/v1/plans           - Create new plan (ADMIN only)
 * - GET    /api/v1/plans           - List all plans
 * - GET    /api/v1/plans/{id}      - Get plan by ID
 * - PUT    /api/v1/plans/{id}      - Update plan (ADMIN only)
 * - DELETE /api/v1/plans/{id}      - Delete plan (ADMIN only)
 * 
 * All endpoints require authentication.
 * Create, update, and delete operations require ROLE_ADMIN.
 * 
 * @see PlanService for business logic
 */
@RestController
@RequestMapping("/api/v1/plans")
@Tag(name = "Billing Plans", description = "Endpoints for managing billing plans (Free, Pro, Enterprise)")
@SecurityRequirement(name = "bearer-jwt")
public class PlanController {

    private static final Logger logger = LoggerFactory.getLogger(PlanController.class);

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    /**
     * Creates a new billing plan.
     * Requires ROLE_ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new billing plan", 
               description = "Creates a new billing plan with pricing and feature limits. Requires ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Plan created successfully",
                     content = @Content(schema = @Schema(implementation = PlanResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
        @ApiResponse(responseCode = "409", description = "Plan with this name already exists")
    })
    public ResponseEntity<PlanResponse> createPlan(
            @Valid @RequestBody CreatePlanRequest request) {
        
        logger.info("Creating new plan: {}", request.getName());

        // Convert DTO to entity
        BillingPlan plan = new BillingPlan(
            request.getName(),
            request.getDescription(),
            request.getPrice(),
            BillingCycle.valueOf(request.getBillingCycle())
        );
        plan.setIsActive(request.getIsActive());

        // Add feature limits
        if (request.getFeatureLimits() != null) {
            for (FeatureLimitRequest limitReq : request.getFeatureLimits()) {
                FeatureLimit limit = new FeatureLimit(limitReq.getLimitType(), limitReq.getLimitValue());
                plan.addFeatureLimit(limit);
            }
        }

        BillingPlan created = planService.createPlan(plan);
        PlanResponse response = PlanResponse.fromEntity(created);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all billing plans.
     */
    @GetMapping
    @Operation(summary = "Get all billing plans", 
               description = "Retrieves all active billing plans. Results are cached for performance.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plans retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid")
    })
    public ResponseEntity<List<PlanResponse>> getAllPlans() {
        logger.debug("Fetching all plans");

        List<BillingPlan> plans = planService.getAllPlans();
        List<PlanResponse> response = plans.stream()
                .map(PlanResponse::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a billing plan by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get plan by ID", 
               description = "Retrieves a specific billing plan by its UUID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plan retrieved successfully",
                     content = @Content(schema = @Schema(implementation = PlanResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "404", description = "Plan not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PlanResponse> getPlanById(
            @Parameter(description = "Plan UUID", required = true)
            @PathVariable UUID id) {
        
        logger.debug("Fetching plan by ID: {}", id);

        BillingPlan plan = planService.getPlanById(id);
        PlanResponse response = PlanResponse.fromEntity(plan);

        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing billing plan.
     * Requires ROLE_ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a billing plan", 
               description = "Updates an existing billing plan. Only provided fields will be updated. Requires ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Plan updated successfully",
                     content = @Content(schema = @Schema(implementation = PlanResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
        @ApiResponse(responseCode = "404", description = "Plan not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PlanResponse> updatePlan(
            @Parameter(description = "Plan UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePlanRequest request) {
        
        logger.info("Updating plan: {}", id);

        // Convert DTO to entity
        BillingPlan updates = new BillingPlan();
        updates.setDescription(request.getDescription());
        updates.setPrice(request.getPrice());
        updates.setIsActive(request.getIsActive());

        // Update feature limits if provided
        if (request.getFeatureLimits() != null) {
            List<FeatureLimit> limits = request.getFeatureLimits().stream()
                    .map(limitReq -> new FeatureLimit(limitReq.getLimitType(), limitReq.getLimitValue()))
                    .collect(Collectors.toList());
            updates.setFeatureLimits(limits);
        }

        BillingPlan updated = planService.updatePlan(id, updates);
        PlanResponse response = PlanResponse.fromEntity(updated);

        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a billing plan (soft delete).
     * Requires ROLE_ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a billing plan", 
               description = "Soft deletes a billing plan. Plans with active subscriptions cannot be deleted. Requires ADMIN role.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Plan deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN role"),
        @ApiResponse(responseCode = "404", description = "Plan not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Plan has active subscriptions and cannot be deleted",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> deletePlan(
            @Parameter(description = "Plan UUID", required = true)
            @PathVariable UUID id) {
        
        logger.info("Deleting plan: {}", id);

        planService.deletePlan(id);

        return ResponseEntity.noContent().build();
    }
}
