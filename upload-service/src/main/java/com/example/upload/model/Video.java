package com.example.upload.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Table;

@Entity
@Table(name = "videos")
public class Video {
    @Id
    private String id = UUID.randomUUID().toString();
    private String filename;
    private long size;
    private String contentType;
    private Instant uploadedAt = Instant.now();
    private Instant processedAt;
    private String failureReason;

    @Enumerated(EnumType.STRING)
    private Status status = Status.UPLOADED;

    public enum Status { UPLOADED, PROCESSING, READY, FAILED }

    // getters/setters
    public String getId() { return id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public Instant getUploadedAt() { return uploadedAt; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}