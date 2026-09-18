package com.nlu.applicationProcess.api.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ResumeUploadInitiateRequest(
        @NotBlank(message = "File name is required")
        String fileName,

        @NotBlank(message = "Content type is required")
        String contentType,

        @NotNull(message = "Size is required")
        @Positive(message = "Size must be greater than 0")
        Long size
) {}
