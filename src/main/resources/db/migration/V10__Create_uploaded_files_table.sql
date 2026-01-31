-- Migration: Create uploaded_files table for file upload tracking
-- Description: Stores metadata for all uploaded files with audit trail and multi-tenant support

CREATE TABLE uploaded_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    uploaded_by VARCHAR(255) NOT NULL,
    tenant_id BIGINT,
    entity_type VARCHAR(50),
    entity_id VARCHAR(100),
    download_url VARCHAR(500) NOT NULL,
    description TEXT,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted BOOLEAN DEFAULT FALSE
);

-- Create indexes for performance
CREATE INDEX idx_uploaded_files_uploaded_by ON uploaded_files(uploaded_by);
CREATE INDEX idx_uploaded_files_entity_type ON uploaded_files(entity_type);
CREATE INDEX idx_uploaded_files_entity_id ON uploaded_files(entity_id);
CREATE INDEX idx_uploaded_files_tenant_id ON uploaded_files(tenant_id);
CREATE INDEX idx_uploaded_files_uploaded_at ON uploaded_files(uploaded_at DESC);

-- Comments for documentation
COMMENT ON TABLE uploaded_files IS 'Tracks all uploaded files with metadata and audit trail';
COMMENT ON COLUMN uploaded_files.filename IS 'Unique UUID-based filename stored on disk';
COMMENT ON COLUMN uploaded_files.original_filename IS 'Original filename provided by user';
COMMENT ON COLUMN uploaded_files.content_type IS 'MIME type of the file';
COMMENT ON COLUMN uploaded_files.file_size IS 'File size in bytes';
COMMENT ON COLUMN uploaded_files.uploaded_by IS 'User email or ID who uploaded the file';
COMMENT ON COLUMN uploaded_files.tenant_id IS 'Tenant ID for multi-tenant isolation';
COMMENT ON COLUMN uploaded_files.entity_type IS 'Type of entity: INVOICE, CUSTOMER, DISPUTE, etc.';
COMMENT ON COLUMN uploaded_files.entity_id IS 'ID of the related entity';
COMMENT ON COLUMN uploaded_files.is_deleted IS 'Soft delete flag';
