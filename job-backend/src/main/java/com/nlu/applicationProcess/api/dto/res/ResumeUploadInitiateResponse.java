package com.nlu.applicationProcess.api.dto.res;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record ResumeUploadInitiateResponse(
        UUID uploadId,
        String uploadUrl,
        String method,
        Map<String, String> requiredHeaders,
        LocalDateTime expiresAt
) {}
