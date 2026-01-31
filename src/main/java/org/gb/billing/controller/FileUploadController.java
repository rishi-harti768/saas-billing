package org.gb.billing.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.gb.billing.dto.response.ErrorResponse;
import org.gb.billing.dto.response.FileUploadResponse;
import org.gb.billing.entity.UploadedFile;
import org.gb.billing.exception.FileStorageException;
import org.gb.billing.security.CustomUserDetails;
import org.gb.billing.service.FileUploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for file upload operations with database persistence.
 * Supports uploading documents (invoices, receipts, user documents) with metadata tracking.
 * 
 * Implements mandatory PRD requirement: File Upload feature.
 */
@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "File Upload", description = "File upload and download endpoints for documents, invoices, and receipts")
@SecurityRequirement(name = "Bearer Authentication")
public class FileUploadController {

    private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * Upload a single file.
     *
     * @param file the file to upload (PDF, JPG, PNG, DOC, DOCX)
     * @return response with uploaded file details
     */
    @Operation(
            summary = "Upload a single file",
            description = "Upload a document (invoice, receipt, or user document). " +
                    "Supports: PDF, JPG, JPEG, PNG, DOC, DOCX. Max size: 5MB."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File uploaded successfully",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid file or validation error",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - JWT token missing or invalid",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "entityType", required = false) String entityType,
            @RequestParam(value = "entityId", required = false) String entityId,
            @RequestParam(value = "description", required = false) String description,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        logger.info("File upload request received: {} by user: {}", 
                   file.getOriginalFilename(), userDetails.getEmail());

        try {
            UploadedFile uploadedFile;
            
            if (entityType != null && entityId != null) {
                // Store with entity association
                uploadedFile = fileUploadService.storeFileWithEntity(
                    file,
                    userDetails.getEmail(),
                    userDetails.getTenantId(),
                    entityType,
                    entityId,
                    description
                );
            } else {
                // Store without entity association
                uploadedFile = fileUploadService.storeFile(
                    file,
                    userDetails.getEmail(),
                    userDetails.getTenantId()
                );
            }

            FileUploadResponse response = new FileUploadResponse(
                    uploadedFile.getFilename(),
                    uploadedFile.getOriginalFilename(),
                    uploadedFile.getContentType(),
                    uploadedFile.getFileSize(),
                    uploadedFile.getDownloadUrl()
            );

            logger.info("File uploaded successfully: {}", uploadedFile.getFilename());
            return ResponseEntity.ok(response);

        } catch (FileStorageException e) {
            logger.error("File upload failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Upload multiple files at once.
     *
     * @param files array of files to upload
     * @return list of uploaded file details
     */
    @Operation(
            summary = "Upload multiple files",
            description = "Upload multiple documents at once. Each file must meet individual validation criteria."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Files uploaded successfully",
                    content = @Content(schema = @Schema(implementation = FileUploadResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "One or more files failed validation",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<List<FileUploadResponse>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        logger.info("Multiple file upload request received: {} files by user: {}", 
                   files.length, userDetails.getEmail());

        List<FileUploadResponse> responses = new java.util.ArrayList<>();
        
        for (MultipartFile file : files) {
            UploadedFile uploadedFile = fileUploadService.storeFile(
                file,
                userDetails.getEmail(),
                userDetails.getTenantId()
            );

            responses.add(new FileUploadResponse(
                    uploadedFile.getFilename(),
                    uploadedFile.getOriginalFilename(),
                    uploadedFile.getContentType(),
                    uploadedFile.getFileSize(),
                    uploadedFile.getDownloadUrl()
            ));
        }

        logger.info("Multiple files uploaded successfully: {} files", responses.size());
        return ResponseEntity.ok(responses);
    }

    /**
     * Get all files uploaded by the current user.
     */
    @Operation(
            summary = "Get my uploaded files",
            description = "Retrieve all files uploaded by the authenticated user."
    )
    @GetMapping("/my-files")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<List<FileUploadResponse>> getMyFiles(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        logger.info("Fetching files for user: {}", userDetails.getEmail());

        List<UploadedFile> files = fileUploadService.getFilesByUser(userDetails.getEmail());
        
        List<FileUploadResponse> responses = files.stream()
                .map(f -> new FileUploadResponse(
                        f.getFilename(),
                        f.getOriginalFilename(),
                        f.getContentType(),
                        f.getFileSize(),
                        f.getDownloadUrl()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get files for a specific entity (e.g., all files for an invoice).
     */
    @Operation(
            summary = "Get files by entity",
            description = "Retrieve all files associated with a specific entity (invoice, customer, etc.)."
    )
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<List<FileUploadResponse>> getFilesByEntity(
            @PathVariable String entityType,
            @PathVariable String entityId
    ) {
        logger.info("Fetching files for entity: {} with ID: {}", entityType, entityId);

        List<UploadedFile> files = fileUploadService.getFilesByEntity(entityType, entityId);
        
        List<FileUploadResponse> responses = files.stream()
                .map(f -> new FileUploadResponse(
                        f.getFilename(),
                        f.getOriginalFilename(),
                        f.getContentType(),
                        f.getFileSize(),
                        f.getDownloadUrl()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get storage usage for current tenant.
     */
    @Operation(
            summary = "Get storage usage",
            description = "Get total storage used by the current tenant in MB."
    )
    @GetMapping("/storage-usage")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<StorageUsageResponse> getStorageUsage(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        double usageMB = fileUploadService.getTenantStorageUsage(userDetails.getTenantId());
        return ResponseEntity.ok(new StorageUsageResponse(usageMB));
    }

    // Inner class for storage usage response
    public static class StorageUsageResponse {
        private double usageMB;

        public StorageUsageResponse(double usageMB) {
            this.usageMB = usageMB;
        }

        public double getUsageMB() {
            return usageMB;
        }

        public void setUsageMB(double usageMB) {
            this.usageMB = usageMB;
        }
    }

    /**
     * Download a previously uploaded file.
     *
     * @param filename the filename to download
     * @return the file as a downloadable resource
     */
    @Operation(
            summary = "Download a file",
            description = "Download a previously uploaded file by its filename."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "File downloaded successfully",
                    content = @Content(mediaType = "application/octet-stream")
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "File not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/download/{filename:.+}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_USER')")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        logger.info("File download request: {}", filename);

        try {
            Path filePath = fileUploadService.getFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                logger.error("File not found or not readable: {}", filename);
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = "application/octet-stream";
            try {
                contentType = java.nio.file.Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
            } catch (IOException e) {
                logger.warn("Could not determine file content type for: {}", filename);
            }

            logger.info("File downloaded successfully: {}", filename);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            logger.error("Invalid file path: {}", filename, e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete a file (admin only).
     *
     * @param filename the filename to delete
     * @return success response
     */
    @Operation(
            summary = "Delete a file (Admin only)",
            description = "Delete a file from storage. Only admins can perform this operation."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "File deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "File not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - Admin role required",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{filename:.+}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> deleteFile(@PathVariable String filename) {
        logger.info("File delete request: {}", filename);

        if (!fileUploadService.fileExists(filename)) {
            logger.error("File not found: {}", filename);
            return ResponseEntity.notFound().build();
        }

        fileUploadService.deleteFile(filename);
        logger.info("File deleted successfully: {}", filename);
        return ResponseEntity.noContent().build();
    }
}
