package com.job_web.dto.ai;

public record ResumeParsingMessage(
        String rawText,
        long userId,
        long cvId
) {}