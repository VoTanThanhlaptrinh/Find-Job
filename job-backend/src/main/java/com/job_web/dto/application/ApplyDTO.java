package com.job_web.dto.application;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ApplyDTO(
        @NotNull(message = "Job ID is required.")
        String jobId,

        @NotNull(message = "Resume file is required.")
        MultipartFile file
) {
    public boolean isValidFileSize() {
        return file != null && file.getSize() <= 5 * 1024 * 1024;
    }

    @AssertTrue(message = "Maximum file size is 5 MB.")
    public boolean getValidFileSize() {
        return isValidFileSize();
    }

    public String getJobId() {
        return jobId;
    }

    public MultipartFile getFile() {
        return file;
    }
}
