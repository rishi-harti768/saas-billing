package org.gb.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionCountByPlan {
    private UUID planId;
    private String planName;
    private long activeCount;
}
