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
import org.gb.billing.dto.request.SubscribeRequest;
import org.gb.billing.dto.response.ErrorResponse;
import org.gb.billing.dto.response.SubscriptionResponse;
import org.gb.billing.entity.Subscription;
import org.gb.billing.entity.User;
import org.gb.billing.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for subscription management.
 * 
 * Endpoints:
 * - POST   /api/v1/subscriptions                - Create subscription (USER)
 * - GET    /api/v1/subscriptions/my-subscription - Get current user's subscription (USER)
 * - GET    /api/v1/subscriptions/{id}           - Get subscription by ID (USER)
 * 
 * All endpoints require authentication.
 * User ID and Tenant ID are extracted from JWT token (security context).
 * 
 * @see SubscriptionService for business logic
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@Tag(name = "Subscriptions", description = "Endpoints for managing customer subscriptions")
@SecurityRequirement(name = "bearer-jwt")
public class SubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionController.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    /**
     * Creates a new subscription for the authenticated user.
     * Requires ROLE_USER.
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Subscribe to a billing plan", 
               description = "Creates a new subscription for the authenticated user. User can have only one active subscription at a time.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Subscription created successfully",
                     content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires USER role"),
        @ApiResponse(responseCode = "404", description = "Plan not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "User already has an active subscription",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubscriptionResponse> createSubscription(
            @Valid @RequestBody SubscribeRequest request,
            Authentication authentication) {
        
        User user = getUserFromAuthentication(authentication);
        logger.info("User {} subscribing to plan {}", user.getId(), request.getPlanId());

        Long userId = user.getId();
        Long tenantId = user.getTenantId();

        Subscription subscription = subscriptionService.createSubscription(userId, tenantId, request.getPlanId());
        SubscriptionResponse response = SubscriptionResponse.fromEntity(subscription);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the authenticated user's current subscription.
     */
    @GetMapping("/my-subscription")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get my current subscription", 
               description = "Retrieves the authenticated user's active or past-due subscription.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription retrieved successfully",
                     content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires USER role"),
        @ApiResponse(responseCode = "404", description = "No active subscription found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubscriptionResponse> getMySubscription(
            Authentication authentication) {
        
        User user = getUserFromAuthentication(authentication);
        logger.debug("Fetching subscription for user {}", user.getId());

        Long userId = user.getId();
        Long tenantId = user.getTenantId();

        Optional<Subscription> subscription = subscriptionService.getMySubscription(userId, tenantId);

        if (subscription.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SubscriptionResponse response = SubscriptionResponse.fromEntity(subscription.get());
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a subscription by ID.
     * Enforces tenant isolation - users can only access subscriptions in their tenant.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get subscription by ID", 
               description = "Retrieves a specific subscription by its UUID. Tenant isolation is enforced.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription retrieved successfully",
                     content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires USER role"),
        @ApiResponse(responseCode = "404", description = "Subscription not found or access denied",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubscriptionResponse> getSubscriptionById(
            @Parameter(description = "Subscription UUID", required = true)
            @PathVariable UUID id,
            Authentication authentication) {
        
        User user = getUserFromAuthentication(authentication);
        logger.debug("Fetching subscription {} for user {}", id, user.getId());

        Long tenantId = user.getTenantId();

        Subscription subscription = subscriptionService.getSubscriptionById(id, tenantId);
        SubscriptionResponse response = SubscriptionResponse.fromEntity(subscription);

        return ResponseEntity.ok(response);
    }

    /**
     * Upgrades a subscription to a new plan.
     * Requires ROLE_USER.
     */
    @PutMapping("/{id}/upgrade")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Upgrade subscription to a new plan", 
               description = "Upgrades an existing subscription to a higher-tier plan. Subscription must be in ACTIVE status.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Subscription upgraded successfully",
                     content = @Content(schema = @Schema(implementation = SubscriptionResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid upgrade request (e.g., subscription not ACTIVE, same plan)",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires USER role"),
        @ApiResponse(responseCode = "404", description = "Subscription or plan not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Optimistic locking conflict - subscription was modified concurrently",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<SubscriptionResponse> upgradeSubscription(
            @Parameter(description = "Subscription UUID", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody org.gb.billing.dto.request.UpgradeRequest request,
            Authentication authentication) {
        
        User user = getUserFromAuthentication(authentication);
        logger.info("User {} upgrading subscription {} to plan {}", user.getId(), id, request.getNewPlanId());

        Long tenantId = user.getTenantId();

        Subscription subscription = subscriptionService.upgradeSubscription(id, tenantId, request.getNewPlanId());
        SubscriptionResponse response = SubscriptionResponse.fromEntity(subscription);

        return ResponseEntity.ok(response);
    }

    /**
     * Cancels a subscription.
     * Requires ROLE_USER.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Cancel a subscription", 
               description = "Cancels an active or past-due subscription. Subscription transitions to CANCELED state (terminal).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Subscription canceled successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid cancellation request (e.g., already canceled)",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires USER role"),
        @ApiResponse(responseCode = "404", description = "Subscription not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Optimistic locking conflict - subscription was modified concurrently",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> cancelSubscription(
            @Parameter(description = "Subscription UUID", required = true)
            @PathVariable UUID id,
            Authentication authentication) {
        
        User user = getUserFromAuthentication(authentication);
        logger.info("User {} canceling subscription {}", user.getId(), id);

        Long userId = user.getId();
        Long tenantId = user.getTenantId();

        subscriptionService.cancelSubscription(id, tenantId, userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves transition history for a subscription.
     * Requires ROLE_USER.
     */
    @GetMapping("/{id}/transitions")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "Get subscription transition history", 
               description = "Retrieves the audit log of state transitions for a subscription.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transition history retrieved successfully",
                     content = @Content(schema = @Schema(implementation = org.gb.billing.entity.SubscriptionTransitionLog.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - JWT token missing or invalid"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Requires USER role"),
        @ApiResponse(responseCode = "404", description = "Subscription not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<java.util.List<org.gb.billing.entity.SubscriptionTransitionLog>> getTransitionHistory(
            @Parameter(description = "Subscription UUID", required = true)
            @PathVariable UUID id,
            Authentication authentication) {
        
        User user = getUserFromAuthentication(authentication);
        logger.debug("Fetching transition history for subscription {} and user {}", id, user.getId());

        Long tenantId = user.getTenantId();

        java.util.List<org.gb.billing.entity.SubscriptionTransitionLog> history = 
            subscriptionService.getTransitionHistory(id, tenantId);

        return ResponseEntity.ok(history);
    }

    /**
     * Helper method to extract User from Authentication.
     * Supports both JWT authentication and test mock authentication.
     */
    private User getUserFromAuthentication(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("No authenticated user found");
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            return (User) principal;
        }
        throw new IllegalStateException("Principal is not a User instance: " + principal.getClass());
    }
}

