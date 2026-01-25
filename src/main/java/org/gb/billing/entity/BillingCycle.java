package org.gb.billing.entity;

/**
 * Billing cycle enum representing subscription billing frequency.
 * 
 * Supported billing cycles:
 * - MONTHLY: Billed every month
 * - YEARLY: Billed annually (typically with discount)
 * 
 * Used by BillingPlan to define how often customers are charged.
 */
public enum BillingCycle {
    /**
     * Monthly billing cycle - customer charged every month
     */
    MONTHLY,
    
    /**
     * Yearly billing cycle - customer charged annually
     * Typically offers cost savings compared to 12 monthly payments
     */
    YEARLY
}
