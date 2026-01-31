package org.gb.billing.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity to track uploaded files with metadata and audit trail.
 * Stores information about files uploaded by users for invoices, receipts, disputes, etc.
 */
@Entity
@Table(name = "uploaded_files", indexes = {
        @Index(name = "idx_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_entity_type", columnList = "entity_type"),
        @Index(name = "idx_entity_id", columnList = "entity_id"),
        @Index(name = "idx_tenant_id", columnList = "tenant_id")
})
@EntityListeners(AuditingEntityListener.class)
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "filename", nullable = false, unique = true)
    private String filename; // Stored filename (UUID-based)

    @Column(name = "original_filename", nullable = false)
    private String originalFilename; // User's original filename

    @Column(name = "content_type", nullable = false)
    private String contentType; // MIME type (application/pdf, image/jpeg, etc.)

    @Column(name = "file_size", nullable = false)
    private Long fileSize; // Size in bytes

    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy; // User email or ID who uploaded the file

    @Column(name = "tenant_id")
    private Long tenantId; // Multi-tenant isolation (using Long to match User entity)

    @Column(name = "entity_type")
    private String entityType; // Type of entity: INVOICE, CUSTOMER, DISPUTE, SUBSCRIPTION, GENERAL

    @Column(name = "entity_id")
    private String entityId; // Related entity ID (UUID as string for flexibility)

    @Column(name = "download_url", nullable = false)
    private String downloadUrl; // URL to download the file

    @Column(name = "description")
    private String description; // Optional description

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "is_deleted")
    private boolean isDeleted = false; // Soft delete flag

    // Constructors
    public UploadedFile() {
    }

    public UploadedFile(String filename, String originalFilename, String contentType, Long fileSize,
                        String uploadedBy, Long tenantId, String downloadUrl) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.tenantId = tenantId;
        this.downloadUrl = downloadUrl;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    /**
     * Enum for file entity types (for better type safety).
     */
    public enum EntityType {
        INVOICE,
        CUSTOMER,
        DISPUTE,
        SUBSCRIPTION,
        PAYMENT_RECEIPT,
        GENERAL
    }
}
