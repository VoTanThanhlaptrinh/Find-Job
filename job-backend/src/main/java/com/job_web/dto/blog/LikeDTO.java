package com.job_web.dto.blog;

import jakarta.validation.constraints.NotNull;

public record LikeDTO(
        @NotNull(message = "{validation.blog.id.required}")
        long id
) {
    public long getId() {
        return id;
    }
}
