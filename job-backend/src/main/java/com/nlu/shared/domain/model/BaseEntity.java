package com.nlu.shared.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false)
    private EntityStatus recordStatus;

    @PrePersist
    protected void onPrePersist() {
        if (this.recordStatus == null) {
            this.recordStatus = EntityStatus.ACTIVE;
        }
        // Lưu ý: Không cần gán this.createdAt = LocalDateTime.now() ở đây
        // vì @CreatedDate và AuditingEntityListener đã tự động làm việc đó.
    }

    // --- DOMAIN BEHAVIORS ---
    public void markActive() {
        this.recordStatus = EntityStatus.ACTIVE;
    }

    public void markDeleted() {
        this.recordStatus = EntityStatus.DELETED;
    }

    public boolean isDeleted() {
        return EntityStatus.DELETED.equals(this.recordStatus);
    }
}