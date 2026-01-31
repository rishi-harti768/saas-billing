package org.gb.billing.repository;

import org.gb.billing.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UploadedFile entity.
 * Provides data access methods for file metadata tracking.
 */
@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

    /**
     * Find file by stored filename.
     */
    Optional<UploadedFile> findByFilename(String filename);

    /**
     * Find all files uploaded by a specific user.
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.uploadedBy = :uploadedBy AND f.isDeleted = false ORDER BY f.uploadedAt DESC")
    List<UploadedFile> findByUploadedBy(@Param("uploadedBy") String uploadedBy);

    /**
     * Find all files for a specific entity (e.g., all files for an invoice).
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.entityType = :entityType AND f.entityId = :entityId AND f.isDeleted = false")
    List<UploadedFile> findByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") String entityId);

    /**
     * Find all files for a specific tenant.
     */
    @Query("SELECT f FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.isDeleted = false ORDER BY f.uploadedAt DESC")
    List<UploadedFile> findByTenantId(@Param("tenantId") Long tenantId);

    /**
     * Count files by entity type.
     */
    @Query("SELECT COUNT(f) FROM UploadedFile f WHERE f.entityType = :entityType AND f.isDeleted = false")
    Long countByEntityType(@Param("entityType") String entityType);

    /**
     * Get total storage used by a tenant (in bytes).
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM UploadedFile f WHERE f.tenantId = :tenantId AND f.isDeleted = false")
    Long getTotalStorageByTenant(@Param("tenantId") Long tenantId);
}
