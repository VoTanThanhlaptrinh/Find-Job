package com.nlu.applicationProcess.api.dto.client;

public record ResumeRequest(Long userId,
                            Long cvId,
                            ResumeModel data) {
}
