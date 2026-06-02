package com.job_web.application_process.api.dto;

import java.time.LocalDateTime;

public record ResumeDetailDTO(
        long id,
        String fileName,
        LocalDateTime createDate
) {
    public long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }
}
