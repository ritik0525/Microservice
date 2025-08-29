package com.example.videoms.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "videos")
public class Video {
    @Id
    private String id;

    private String originalFilename;
    private String contentType;
    private long size;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant uploadedAt;
    private Instant processedAt;
    private String failureReason;

    public enum Status { UPLOADED, PROCESSING, READY, FAILED }

    public Video() {
        this.id = UUID.randomUUID().toString();
        this.uploadedAt = Instant.now();
        this.status = Status.UPLOADED;
    }

    // getters and setters omitted for brevity (include all standard getters/setters)
    // ...
    public String getId() { return id; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
}