package org.gb.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueByPlan {
    private UUID planId;
    private String planName;
    private BigDecimal revenueAmount;
    private BigDecimal percentageOfTotal;
}
