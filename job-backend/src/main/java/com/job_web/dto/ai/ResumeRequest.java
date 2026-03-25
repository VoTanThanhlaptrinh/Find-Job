package com.job_web.dto.ai;

public record ResumeRequest(Long userId,
                            Long cvId,
                            ResumeModel data) {
}
