package com.job_web.dto.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ApplyCvWithUploadRequest(
        @NotNull(message = "{validation.apply.jobId.required}")
        Long jobId,

        @NotNull(message = "{validation.apply.cvFile.required}")
        MultipartFile cvFile,

        String coverLetter
) {
    public Long getJobId() {
        return jobId;
    }

    public MultipartFile getCvFile() {
        return cvFile;
    }

    public String getCoverLetter() {
        return coverLetter;
    }
}
