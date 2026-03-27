package com.job_web.dto.application;

import java.time.LocalDateTime;

public record ResumeView(
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
