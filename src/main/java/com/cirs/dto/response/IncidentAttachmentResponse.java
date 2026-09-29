package com.cirs.dto.response;

import java.time.LocalDateTime;

public class IncidentAttachmentResponse {
    private Long id;
    private String originalFilename;
    private Long fileSizeBytes;
    private String mimeType;
    private String sha256Hash;
    private String uploadedByName;
    private LocalDateTime uploadedAt;

    public IncidentAttachmentResponse() {}

    public IncidentAttachmentResponse(Long id, String originalFilename, Long fileSizeBytes,
                                      String mimeType, String sha256Hash, String uploadedByName,
                                      LocalDateTime uploadedAt) {
        this.id = id;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
        this.mimeType = mimeType;
        this.sha256Hash = sha256Hash;
        this.uploadedByName = uploadedByName;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public Long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(Long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    public String getUploadedByName() {
        return uploadedByName;
    }

    public void setUploadedByName(String uploadedByName) {
        this.uploadedByName = uploadedByName;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
