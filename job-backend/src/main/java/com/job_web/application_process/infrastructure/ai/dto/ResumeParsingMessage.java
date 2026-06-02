package com.job_web.application_process.infrastructure.ai.dto;

public record ResumeParsingMessage(
        String rawText,
        long userId,
        long cvId
) {}