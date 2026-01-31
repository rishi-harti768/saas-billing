-- Create tenants table for multi-tenant SaaS system
CREATE TABLE tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT true,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100)
);

-- Create indexes for performance
CREATE INDEX idx_tenants_name ON tenants(name);
CREATE INDEX idx_tenants_active ON tenants(active);

-- Add comments for documentation
COMMENT ON TABLE tenants IS 'Organizations/tenants in the multi-tenant SaaS system';
COMMENT ON COLUMN tenants.id IS 'Primary key';
COMMENT ON COLUMN tenants.name IS 'Tenant/organization name (unique)';
COMMENT ON COLUMN tenants.active IS 'Whether tenant is active (soft delete support)';
COMMENT ON COLUMN tenants.created_date IS 'Timestamp when tenant was created';
COMMENT ON COLUMN tenants.last_modified_date IS 'Timestamp of last modification';
