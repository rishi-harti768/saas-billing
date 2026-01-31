-- Create users table for authentication and authorization
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(60) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ROLE_ADMIN', 'ROLE_USER')),
    tenant_id BIGINT REFERENCES tenants(id) ON DELETE RESTRICT,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT true,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_date TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    created_by VARCHAR(100),
    last_modified_by VARCHAR(100),
    CONSTRAINT chk_user_tenant CHECK (
        (role = 'ROLE_ADMIN' AND tenant_id IS NULL) OR
        (role = 'ROLE_USER' AND tenant_id IS NOT NULL)
    )
);

-- Create indexes for performance
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_tenant_id ON users(tenant_id);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(active);

-- Add comments for documentation
COMMENT ON TABLE users IS 'System users (admins and subscribers)';
COMMENT ON COLUMN users.id IS 'Primary key';
COMMENT ON COLUMN users.email IS 'User email (unique login identifier)';
COMMENT ON COLUMN users.password IS 'BCrypt-hashed password (60 characters)';
COMMENT ON COLUMN users.role IS 'User role: ROLE_ADMIN or ROLE_USER';
COMMENT ON COLUMN users.tenant_id IS 'Tenant association (NULL for admins, NOT NULL for users)';
COMMENT ON COLUMN users.first_name IS 'User first name (optional)';
COMMENT ON COLUMN users.last_name IS 'User last name (optional)';
COMMENT ON COLUMN users.active IS 'Whether user account is active';
COMMENT ON COLUMN users.created_date IS 'Account creation timestamp';
COMMENT ON COLUMN users.last_modified_date IS 'Last modification timestamp';
COMMENT ON COLUMN users.last_login_date IS 'Last successful login timestamp';
COMMENT ON COLUMN users.version IS 'Optimistic locking version for concurrent updates';
COMMENT ON CONSTRAINT chk_user_tenant ON users IS 'Admins have no tenant, users must have tenant';
