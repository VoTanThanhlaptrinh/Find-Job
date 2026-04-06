package com.job_web.dto.application;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ApplyCvWithExistingRequest(
        @NotBlank(message = "{validation.apply.jobId.required}")
        Long jobId,

        @NotBlank(message = "{validation.apply.existingCvId.required}")
        Long existingCvId,

        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
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
