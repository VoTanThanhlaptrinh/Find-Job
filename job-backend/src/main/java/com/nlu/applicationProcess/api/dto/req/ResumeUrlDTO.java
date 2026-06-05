package com.nlu.applicationProcess.api.dto.req;

/**
 * DTO chứa Pre-signed URL cho resume.
 */
public record ResumeUrlDTO(
        long resumeId,
        String fileName,
        String url,
        int expiresInMinutes
) {
}
