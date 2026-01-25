package org.gb.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueSummaryResponse {
    private BigDecimal monthlyRecurringRevenue; // MRR
    private BigDecimal annualRecurringRevenue;  // ARR
    private List<RevenueByPlan> revenueByPlan;
}
