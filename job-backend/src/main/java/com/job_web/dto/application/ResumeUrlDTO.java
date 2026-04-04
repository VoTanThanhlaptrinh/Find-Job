package com.job_web.dto.application;

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
