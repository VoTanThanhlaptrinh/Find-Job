package com.job_web.models;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class StatusEntity {
    @Column(nullable = false)
    private String status;

    @PrePersist
    protected void initStatus() {
        if (status == null) {
            status = EntityStatus.ACTIVE.name();
        }
    }

    public void markActive() {
        status = EntityStatus.ACTIVE.name();
    }

    public void markDeleted() {
        status = EntityStatus.DELETED.name();
    }

    public boolean isDeleted() {
        return EntityStatus.DELETED.name().equals(status);
    }
}
