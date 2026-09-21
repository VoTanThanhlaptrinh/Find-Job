package com.nlu.shared.domain.model;

import java.time.LocalDateTime;
import java.util.Map;

public record PresignedUploadUrlResponse(
        String url,
        String method,
        Map<String, String> requiredHeaders,
        LocalDateTime expiresAt
) {}
