-- Create Free Plan
INSERT INTO billing_plan (name, description, price, billing_cycle, is_active, deleted, created_at, updated_at)
VALUES ('Free', 'Basic plan for individuals', 0.00, 'MONTHLY', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Create Pro Plan
INSERT INTO billing_plan (name, description, price, billing_cycle, is_active, deleted, created_at, updated_at)
VALUES ('Pro', 'Professional plan for small teams', 29.99, 'MONTHLY', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Create Enterprise Plan
INSERT INTO billing_plan (name, description, price, billing_cycle, is_active, deleted, created_at, updated_at)
VALUES ('Enterprise', 'Advanced plan for large organizations', 299.99, 'YEARLY', true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Add Feature Limits for Free Plan
INSERT INTO feature_limit (plan_id, limit_type, limit_value, created_at, updated_at)
SELECT id, 'MAX_USERS', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM billing_plan WHERE name = 'Free';

INSERT INTO feature_limit (plan_id, limit_type, limit_value, created_at, updated_at)
SELECT id, 'MAX_PROJECTS', 3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM billing_plan WHERE name = 'Free';

-- Add Feature Limits for Pro Plan
INSERT INTO feature_limit (plan_id, limit_type, limit_value, created_at, updated_at)
SELECT id, 'MAX_USERS', 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM billing_plan WHERE name = 'Pro';

INSERT INTO feature_limit (plan_id, limit_type, limit_value, created_at, updated_at)
SELECT id, 'MAX_PROJECTS', 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM billing_plan WHERE name = 'Pro';

-- Add Feature Limits for Enterprise Plan
INSERT INTO feature_limit (plan_id, limit_type, limit_value, created_at, updated_at)
SELECT id, 'MAX_USERS', -1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM billing_plan WHERE name = 'Enterprise'; -- Unlimited

INSERT INTO feature_limit (plan_id, limit_type, limit_value, created_at, updated_at)
SELECT id, 'MAX_PROJECTS', -1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM billing_plan WHERE name = 'Enterprise'; -- Unlimited
