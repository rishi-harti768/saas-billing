package org.gb.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.gb.billing.entity.SubscriptionState;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionCountByStatus {
    private SubscriptionState status;
    private long count;
}
