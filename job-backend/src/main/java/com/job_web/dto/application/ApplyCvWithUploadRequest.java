package com.job_web.dto.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record ApplyCvWithUploadRequest(
        @NotBlank(message = "Job ID is required.")
        Long jobId,

        @NotNull(message = "CV file is required.")
        MultipartFile cvFile,

        @NotBlank(message = "Email is required.")
        @Email(message = "Email is invalid.")
        String email,

        String coverLetter
) {
    public Long getJobId() {
        return jobId;
    }

    public MultipartFile getCvFile() {
        return cvFile;
    }

    public String getEmail() {
        return email;
    }

    public String getCoverLetter() {
        return coverLetter;
    }
}
