-- Create payment table for Phase 3
CREATE TABLE payment (
    id UUID PRIMARY KEY,
    subscription_id UUID NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20),
    external_payment_id VARCHAR(255),
    invoice_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    
    CONSTRAINT fk_payment_subscription FOREIGN KEY (subscription_id) REFERENCES subscription(id) ON DELETE CASCADE
);

-- Create indexes for performance
CREATE INDEX idx_payment_subscription ON payment(subscription_id);
CREATE INDEX idx_payment_tenant ON payment(tenant_id);
CREATE INDEX idx_payment_user ON payment(tenant_id, user_id);
CREATE INDEX idx_payment_status ON payment(status);
CREATE INDEX idx_payment_external_id ON payment(external_payment_id);
CREATE INDEX idx_payment_created_at ON payment(created_at DESC);

-- Add comment
COMMENT ON TABLE payment IS 'Stores payment transactions for subscriptions';
