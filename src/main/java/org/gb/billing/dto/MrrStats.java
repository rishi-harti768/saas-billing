package org.gb.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Monthly Recurring Revenue Response")
public record MrrStats(
    @Schema(description = "Total active MRR across all plans", example = "5000.00")
    BigDecimal totalMrr,
    
    @Schema(description = "Breakdown of revenue by month")
    List<RevenueStats> monthlyBreakdown
) {}
