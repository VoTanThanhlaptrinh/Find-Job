package com.nlu.applicationProcess.api.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nlu.applicationProcess.domain.model.ResumeStatus;

import java.time.LocalDateTime;

public record ResumeDetailDTO(
        long id,
        String fileName,
        LocalDateTime createDate,
        ResumeStatus status
) {
    public ResumeDetailDTO(long id, String fileName, LocalDateTime createDate) {
        this(id, fileName, createDate, ResumeStatus.UPLOADED);
    }

    public long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }

    public ResumeStatus getStatus() {
        return status;
    }

    @JsonProperty("isAnalyzed")
    public boolean isAnalyzed() {
        return status == ResumeStatus.READY;
    }
}
