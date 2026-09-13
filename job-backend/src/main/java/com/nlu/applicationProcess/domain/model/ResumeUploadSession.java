package com.nlu.applicationProcess.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resume_upload_session", indexes = {
        @Index(name = "idx_resume_upload_session_user_id", columnList = "user_id"),
        @Index(name = "idx_resume_upload_session_status_expires", columnList = "status, expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeUploadSession {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "declared_content_type", nullable = false, length = 128)
    private String declaredContentType;

    @Column(name = "declared_size", nullable = false)
    private long declaredSize;

    @Column(name = "temp_key", nullable = false, unique = true, length = 512)
    private String tempKey;

    @Column(name = "permanent_key", nullable = false, unique = true, length = 512)
    private String permanentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ResumeUploadSessionStatus status;

    @Column(name = "resume_id", unique = true)
    private Long resumeId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
