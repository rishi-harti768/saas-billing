package org.gb.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Customer Churn Statistics")
public record ChurnStats(
    @Schema(description = "Total number of subscriptions ever created", example = "100")
    long totalSubscribers,

    @Schema(description = "Total number of cancelled subscriptions", example = "5")
    long cancelledSubscribers,

    @Schema(description = "Churn rate percentage (Cancelled / Total * 100)", example = "5.0")
    double churnRatePercentage
) {}
