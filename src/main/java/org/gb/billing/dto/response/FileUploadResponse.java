package org.gb.billing.dto.response;

/**
 * Response DTO for file upload operations.
 */
public class FileUploadResponse {

    private String filename;
    private String originalFilename;
    private String contentType;
    private long size;
    private String downloadUrl;

    public FileUploadResponse() {
    }

    public FileUploadResponse(String filename, String originalFilename, String contentType, long size, String downloadUrl) {
        this.filename = filename;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
        this.downloadUrl = downloadUrl;
    }

    // Getters and Setters

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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }
}
