package com.job_web.application_process.api.dto;

import jakarta.validation.constraints.*;

public record ApplyCvWithExistingRequest(
        @NotNull(message = "{validation.apply.jobId.required}")
        Long jobId,

        @NotNull(message = "{validation.apply.existingCvId.required}")
        Long existingCvId,

        String coverLetter
) {
}
