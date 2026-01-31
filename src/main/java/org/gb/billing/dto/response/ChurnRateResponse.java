package org.gb.billing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
public class ChurnRateResponse {
    private Double churnRatePercentage;
    private long churnedSubscriptions;
    private long totalSubscriptionsStartOfPeriod;
    private String period; // e.g., "LAST_30_DAYS"

    public ChurnRateResponse() {}

    public ChurnRateResponse(Double churnRatePercentage, long churnedSubscriptions, long totalSubscriptionsStartOfPeriod, String period) {
        this.churnRatePercentage = churnRatePercentage;
        this.churnedSubscriptions = churnedSubscriptions;
        this.totalSubscriptionsStartOfPeriod = totalSubscriptionsStartOfPeriod;
        this.period = period;
    }

    public Double getChurnRatePercentage() { return churnRatePercentage; }
    public void setChurnRatePercentage(Double churnRatePercentage) { this.churnRatePercentage = churnRatePercentage; }
    public long getChurnedSubscriptions() { return churnedSubscriptions; }
    public void setChurnedSubscriptions(long churnedSubscriptions) { this.churnedSubscriptions = churnedSubscriptions; }
    public long getTotalSubscriptionsStartOfPeriod() { return totalSubscriptionsStartOfPeriod; }
    public void setTotalSubscriptionsStartOfPeriod(long totalSubscriptionsStartOfPeriod) { this.totalSubscriptionsStartOfPeriod = totalSubscriptionsStartOfPeriod; }
    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
}
