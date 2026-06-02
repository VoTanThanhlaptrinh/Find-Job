package com.job_web.application_process.infrastructure.ai.dto;

public record ResumeRequest(Long userId,
                            Long cvId,
                            ResumeModel data) {
}
