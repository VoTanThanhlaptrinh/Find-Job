package com.nlu.applicationProcess.api.dto.req;

import java.time.LocalDateTime;

public record ResumeView(
        long id,
        String fileName,
        LocalDateTime createDate,
        boolean isAnalyzed
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

    public boolean isAnalyzed() {
        return isAnalyzed;
    }
}
