package com.nlu.applicationProcess.api.dto.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nlu.applicationProcess.domain.model.ResumeStatus;

import java.time.LocalDateTime;

public record ResumeView(
        long id,
        String fileName,
        LocalDateTime createDate,
        ResumeStatus status
) {
    public ResumeView(long id, String fileName, LocalDateTime createDate, boolean isAnalyzed) {
        this(id, fileName, createDate, isAnalyzed ? ResumeStatus.READY : ResumeStatus.UPLOADED);
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
