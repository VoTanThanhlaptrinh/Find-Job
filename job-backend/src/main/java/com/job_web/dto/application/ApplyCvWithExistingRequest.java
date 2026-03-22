package com.job_web.dto.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ApplyCvWithExistingRequest(
        @NotBlank(message = "Job ID is required.")
        Long jobId,

        @NotBlank(message = "Existing CV ID is required.")
        Long existingCvId,

        @NotBlank(message = "Email is required.")
        @Email(message = "Email is invalid.")
        String email,

        String coverLetter
) {
    public Long getJobId() {
        return jobId;
    }

    public Long getExistingCvId() {
        return existingCvId;
    }

    public String getEmail() {
        return email;
    }

    public String getCoverLetter() {
        return coverLetter;
    }
}
