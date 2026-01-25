package org.gb.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChurnRateResponse {
    private Double churnRatePercentage;
    private long churnedSubscriptions;
    private long totalSubscriptionsStartOfPeriod;
    private String period; // e.g., "LAST_30_DAYS"
}
