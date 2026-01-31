package org.gb.billing.service;

import org.gb.billing.entity.UploadedFile;
import org.gb.billing.exception.FileStorageException;
import org.gb.billing.repository.UploadedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Service for handling file uploads with database persistence.
 * Supports document uploads (PDF, images) for invoices, receipts, and user documents.
 * Files are stored locally with metadata tracked in database for audit trail.
 */
@Service
public class FileUploadService {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadService.class);

    private final UploadedFileRepository uploadedFileRepository;
    private final Path fileStorageLocation;

    // Allowed file extensions for security
    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "pdf", "jpg", "jpeg", "png", "doc", "docx"
    );

    // Maximum file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    public FileUploadService(@Value("${file.upload.dir:./uploads}") String uploadDir,
                            UploadedFileRepository uploadedFileRepository) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.uploadedFileRepository = uploadedFileRepository;
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            logger.info("File upload directory initialized: {}", this.fileStorageLocation);
        } catch (IOException e) {
            throw new FileStorageException("Could not create upload directory", e);
        }
    }

    /**
     * Store a single uploaded file with metadata persistence.
     *
     * @param file the multipart file to store
     * @param uploadedBy email/ID of the user uploading the file
     * @param tenantId tenant ID for multi-tenant isolation
     * @return the uploaded file metadata entity
     */
    @Transactional
    public UploadedFile storeFile(MultipartFile file, String uploadedBy, Long tenantId) {
        // Validate file
        validateFile(file);

        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID() + "_" + Instant.now().toEpochMilli() + "." + fileExtension;

        try {
            // Check for path traversal attack
            if (originalFilename.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + originalFilename);
            }

            // Copy file to target location
            Path targetLocation = this.fileStorageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Create database record
            String downloadUrl = "/api/v1/files/download/" + newFilename;
            UploadedFile uploadedFile = new UploadedFile(
                    newFilename,
                    originalFilename,
                    file.getContentType(),
                    file.getSize(),
                    uploadedBy,
                    tenantId,
                    downloadUrl
            );

            // Save to database
            uploadedFile = uploadedFileRepository.save(uploadedFile);

            logger.info("File uploaded successfully: {} (original: {}) by user: {}", 
                       newFilename, originalFilename, uploadedBy);
            return uploadedFile;

        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }
    }

    /**
     * Store file with entity association (e.g., link to invoice, customer, etc.).
     */
    @Transactional
    public UploadedFile storeFileWithEntity(MultipartFile file, String uploadedBy, Long tenantId,
                                           String entityType, String entityId, String description) {
        UploadedFile uploadedFile = storeFile(file, uploadedBy, tenantId);
        uploadedFile.setEntityType(entityType);
        uploadedFile.setEntityId(entityId);
        uploadedFile.setDescription(description);
        return uploadedFileRepository.save(uploadedFile);
    }

    /**
     * Legacy method for backward compatibility (without database persistence).
     * @deprecated Use storeFile(MultipartFile, String, UUID) instead
     */
    @Deprecated
    public String storeFile(MultipartFile file) {
        validateFile(file);
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID() + "_" + Instant.now().toEpochMilli() + "." + fileExtension;

        try {
            if (originalFilename.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + originalFilename);
            }
            Path targetLocation = this.fileStorageLocation.resolve(newFilename);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            logger.info("File uploaded successfully: {} (original: {})", newFilename, originalFilename);
            return newFilename;
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file: " + originalFilename, e);
        }
    }

    /**
     * Store multiple files at once.
     *
     * @param files array of multipart files
     * @return list of stored file paths
     * @deprecated Use new method with user and tenant info
     */
    @Deprecated
    public List<String> storeMultipleFiles(MultipartFile[] files) {
        List<String> storedFiles = new ArrayList<>();
        
        for (MultipartFile file : files) {
            String storedFile = storeFile(file);
            storedFiles.add(storedFile);
        }
        
        logger.info("Stored {} files successfully", storedFiles.size());
        return storedFiles;
    }

    /**
     * Validate file before upload.
     * Checks: not empty, file size, file extension.
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new FileStorageException("Cannot upload empty file");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new FileStorageException(
                    String.format("File size exceeds maximum limit of %d MB", MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Check file extension
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new FileStorageException("File must have a valid name");
        }

        String fileExtension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            throw new FileStorageException(
                    "File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS)
            );
        }
    }

    /**
     * Extract file extension from filename.
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new FileStorageException("File must have an extension");
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * Get the full path to a stored file.
     *
     * @param filename the filename (not full path)
     * @return the full path to the file
     */
    public Path getFilePath(String filename) {
        return this.fileStorageLocation.resolve(filename).normalize();
    }

    /**
     * Delete a file from storage (soft delete in database, physical delete from disk).
     *
     * @param filename the filename to delete
     */
    @Transactional
    public void deleteFile(String filename) {
        try {
            // Soft delete in database
            uploadedFileRepository.findByFilename(filename).ifPresent(file -> {
                file.setDeleted(true);
                uploadedFileRepository.save(file);
                logger.info("File marked as deleted in database: {}", filename);
            });

            // Physical delete from disk
            Path filePath = getFilePath(filename);
            Files.deleteIfExists(filePath);
            logger.info("File deleted from disk: {}", filename);
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", filename, e);
            throw new FileStorageException("Failed to delete file: " + filename, e);
        }
    }

    /**
     * Get file metadata by filename.
     */
    public UploadedFile getFileMetadata(String filename) {
        return uploadedFileRepository.findByFilename(filename)
                .orElseThrow(() -> new FileStorageException("File not found: " + filename));
    }

    /**
     * Get all files uploaded by a specific user.
     */
    public List<UploadedFile> getFilesByUser(String uploadedBy) {
        return uploadedFileRepository.findByUploadedBy(uploadedBy);
    }

    /**
     * Get all files for a specific entity (e.g., all files for an invoice).
     */
    public List<UploadedFile> getFilesByEntity(String entityType, String entityId) {
        return uploadedFileRepository.findByEntityTypeAndEntityId(entityType, entityId);
    }

    /**
     * Get total storage used by a tenant (in MB).
     */
    public double getTenantStorageUsage(Long tenantId) {
        Long bytes = uploadedFileRepository.getTotalStorageByTenant(tenantId);
        return bytes / (1024.0 * 1024.0); // Convert to MB
    }

    /**
     * Check if a file exists.
     */
    public boolean fileExists(String filename) {
        return Files.exists(getFilePath(filename));
    }
}
