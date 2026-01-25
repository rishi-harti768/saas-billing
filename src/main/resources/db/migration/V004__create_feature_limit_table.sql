-- V004: Create feature_limit table for plan feature restrictions
-- Defines what each plan allows (e.g., max_users, max_projects)

CREATE TABLE feature_limit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id UUID NOT NULL,
    limit_type VARCHAR(50) NOT NULL,
    limit_value INTEGER NOT NULL CHECK (limit_value >= -1),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign key to billing_plan
    CONSTRAINT fk_feature_limit_plan FOREIGN KEY (plan_id) 
        REFERENCES billing_plan(id) ON DELETE CASCADE,
    
    -- Business constraints
    CONSTRAINT unique_plan_limit_type UNIQUE (plan_id, limit_type)
);

-- Indexes for performance
CREATE INDEX idx_feature_limit_plan_id ON feature_limit(plan_id);
CREATE INDEX idx_feature_limit_type ON feature_limit(limit_type);

-- Comments for documentation
COMMENT ON TABLE feature_limit IS 'Feature limits for each billing plan (e.g., max_users, max_projects)';
COMMENT ON COLUMN feature_limit.limit_type IS 'Feature identifier (e.g., max_users, max_storage_gb)';
COMMENT ON COLUMN feature_limit.limit_value IS 'Limit value (-1 for unlimited, 0+ for specific limit)';
