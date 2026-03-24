package com.job_web.dto.application;

import java.time.LocalDateTime;

public record ResumeView(
        long id,
        String fitleName,
        LocalDateTime createDate
) {
    public long getId() {
        return id;
    }

    public String getFitleName() {
        return fitleName;
    }

    public LocalDateTime getCreateDate() {
        return createDate;
    }
}
