-- V005: Create subscription table for customer subscriptions
-- Supports subscription lifecycle (ACTIVE, PAST_DUE, CANCELED) with optimistic locking

CREATE TABLE subscription (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    plan_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'PAST_DUE', 'CANCELED')),
    start_date TIMESTAMP NOT NULL,
    next_billing_date TIMESTAMP,
    canceled_at TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign keys
    CONSTRAINT fk_subscription_plan FOREIGN KEY (plan_id) 
        REFERENCES billing_plan(id) ON DELETE RESTRICT,
    
    -- Business constraints
    CONSTRAINT unique_active_subscription_per_user 
        UNIQUE (user_id, tenant_id) 
        WHERE status IN ('ACTIVE', 'PAST_DUE')
);

-- Indexes for performance and tenant isolation
CREATE INDEX idx_subscription_user_id ON subscription(user_id);
CREATE INDEX idx_subscription_tenant_id ON subscription(tenant_id);
CREATE INDEX idx_subscription_status ON subscription(status);
CREATE INDEX idx_subscription_tenant_status ON subscription(tenant_id, status);
CREATE INDEX idx_subscription_plan_id ON subscription(plan_id);
CREATE INDEX idx_subscription_next_billing ON subscription(next_billing_date) WHERE next_billing_date IS NOT NULL;

-- Comments for documentation
COMMENT ON TABLE subscription IS 'Customer subscriptions with lifecycle management and optimistic locking';
COMMENT ON COLUMN subscription.status IS 'Subscription state: ACTIVE, PAST_DUE, CANCELED';
COMMENT ON COLUMN subscription.version IS 'Optimistic locking version for concurrent state transitions';
COMMENT ON COLUMN subscription.next_billing_date IS 'Next billing date (NULL for canceled subscriptions)';
COMMENT ON CONSTRAINT unique_active_subscription_per_user ON subscription IS 'Prevents multiple active subscriptions per user';
