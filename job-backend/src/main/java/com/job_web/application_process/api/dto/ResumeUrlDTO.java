package com.job_web.application_process.api.dto;

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
