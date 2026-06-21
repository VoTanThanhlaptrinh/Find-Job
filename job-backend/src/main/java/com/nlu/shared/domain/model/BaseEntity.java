package com.nlu.shared.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column()
    private LocalDateTime updatedAt;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = true)
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