package com.job_web.dto.application;

import jakarta.validation.constraints.*;
import lombok.Getter;

public record ApplyCvWithExistingRequest(
        @NotNull(message = "{validation.apply.jobId.required}")
        Long jobId,

        @NotNull(message = "{validation.apply.existingCvId.required}")
        Long existingCvId,

        String coverLetter
) {
}
