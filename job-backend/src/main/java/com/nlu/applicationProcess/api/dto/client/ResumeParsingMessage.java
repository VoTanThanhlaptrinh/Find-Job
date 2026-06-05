package com.nlu.applicationProcess.api.dto.client;

public record ResumeParsingMessage(
        String rawText,
        long userId,
        long cvId
) {}