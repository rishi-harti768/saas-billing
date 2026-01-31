package org.gb.billing.dto.response;

import java.time.LocalDate;

public class SubscriptionGrowthData {
    private LocalDate date;
    private long newSubscriptions;
    private long canceledSubscriptions;
    private long netGrowth;
    
    public SubscriptionGrowthData() {
    }
    
    public SubscriptionGrowthData(LocalDate date, long newSubscriptions, long canceledSubscriptions, long netGrowth) {
        this.date = date;
        this.newSubscriptions = newSubscriptions;
        this.canceledSubscriptions = canceledSubscriptions;
        this.netGrowth = netGrowth;
    }
    
    public LocalDate getDate() {
        return date;
    }
    
    public void setDate(LocalDate date) {
        this.date = date;
    }
    
    public long getNewSubscriptions() {
        return newSubscriptions;
    }
    
    public void setNewSubscriptions(long newSubscriptions) {
        this.newSubscriptions = newSubscriptions;
    }
    
    public long getCanceledSubscriptions() {
        return canceledSubscriptions;
    }
    
    public void setCanceledSubscriptions(long canceledSubscriptions) {
        this.canceledSubscriptions = canceledSubscriptions;
    }
    
    public long getNetGrowth() {
        return netGrowth;
    }
    
    public void setNetGrowth(long netGrowth) {
        this.netGrowth = netGrowth;
    }
}
