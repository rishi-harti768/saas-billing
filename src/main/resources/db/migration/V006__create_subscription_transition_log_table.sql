-- V006: Create subscription_transition_log table for audit trail
-- Logs all subscription state transitions for compliance and debugging

CREATE TABLE subscription_transition_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    reason VARCHAR(255),
    transitioned_by UUID,
    transitioned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to subscription
    CONSTRAINT fk_transition_log_subscription FOREIGN KEY (subscription_id) 
        REFERENCES subscription(id) ON DELETE CASCADE
);

-- Indexes for audit queries
CREATE INDEX idx_transition_log_subscription_id ON subscription_transition_log(subscription_id);
CREATE INDEX idx_transition_log_transitioned_at ON subscription_transition_log(transitioned_at);
CREATE INDEX idx_transition_log_to_status ON subscription_transition_log(to_status);

-- Comments for documentation
COMMENT ON TABLE subscription_transition_log IS 'Audit log for all subscription state transitions';
COMMENT ON COLUMN subscription_transition_log.from_status IS 'Previous subscription status (NULL for initial creation)';
COMMENT ON COLUMN subscription_transition_log.to_status IS 'New subscription status';
COMMENT ON COLUMN subscription_transition_log.reason IS 'Reason for transition (e.g., "Payment failed", "User canceled")';
COMMENT ON COLUMN subscription_transition_log.transitioned_by IS 'User ID who triggered the transition (NULL for system transitions)';
