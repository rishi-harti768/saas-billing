package org.gb.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionGrowthData {
    private LocalDate date;
    private long newSubscriptions;
    private long canceledSubscriptions;
    private long netGrowth;
}
