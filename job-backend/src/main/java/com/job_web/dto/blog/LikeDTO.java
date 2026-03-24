package com.job_web.dto.blog;

import jakarta.validation.constraints.NotNull;

public record LikeDTO(
        @NotNull(message = "Blog id khÃ´ng Ä‘Æ°á»£c null")
        long id
) {
    public long getId() {
        return id;
    }
}
