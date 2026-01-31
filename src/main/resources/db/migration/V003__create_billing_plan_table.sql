-- V003: Create billing_plan table for managing subscription plans
-- Supports multi-tenant SaaS billing with soft delete

CREATE TABLE billing_plan (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(19, 4) NOT NULL CHECK (price >= 0),
    billing_cycle VARCHAR(20) NOT NULL CHECK (billing_cycle IN ('MONTHLY', 'YEARLY')),
    is_active BOOLEAN NOT NULL DEFAULT true,
    deleted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    
    -- Business constraints
    CONSTRAINT unique_plan_name UNIQUE (name)
);

-- Indexes for performance
CREATE INDEX idx_billing_plan_deleted ON billing_plan(deleted) WHERE deleted = false;
CREATE INDEX idx_billing_plan_active ON billing_plan(is_active) WHERE is_active = true;
CREATE INDEX idx_billing_plan_name ON billing_plan(name);

-- Comments for documentation
COMMENT ON TABLE billing_plan IS 'Billing plans (Free, Pro, Enterprise) with pricing and billing cycles';
COMMENT ON COLUMN billing_plan.price IS 'Plan price in USD (BigDecimal for precision)';
COMMENT ON COLUMN billing_plan.billing_cycle IS 'MONTHLY or YEARLY billing frequency';
COMMENT ON COLUMN billing_plan.deleted IS 'Soft delete flag - prevents deletion if active subscriptions exist';
