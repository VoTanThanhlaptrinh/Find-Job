package com.nlu.applicationProcess.api.dto.req;

import jakarta.validation.constraints.*;

public record ApplyCvWithExistingRequest(
        @NotNull(message = "{validation.apply.jobId.required}")
        Long jobId,

        @NotNull(message = "{validation.apply.existingCvId.required}")
        Long existingCvId,

        String coverLetter
) {
}
