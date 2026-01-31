package org.gb.billing.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.gb.billing.entity.SubscriptionState;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SubscriptionCountByStatus {
    private SubscriptionState status;
    private long count;

    public SubscriptionCountByStatus() {}
    
    // Manual constructor to work around Lombok annotation processing issue
    public SubscriptionCountByStatus(SubscriptionState status, long count) {
        this.status = status;
        this.count = count;
    }

    public SubscriptionState getStatus() { return status; }
    public void setStatus(SubscriptionState status) { this.status = status; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}
