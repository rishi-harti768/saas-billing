package org.gb.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(description = "Monthly revenue statistics")
public interface RevenueStats {
    @Schema(description = "Billing period in YYYY-MM format", example = "2023-10")
    String getPeriod(); // e.g. "2023-10"

    @Schema(description = "Total revenue for the period", example = "1500.00")
    BigDecimal getAmount();
}
