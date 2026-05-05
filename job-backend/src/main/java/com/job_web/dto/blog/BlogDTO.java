package com.job_web.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlogDTO(
        @NotBlank(message = "{validation.blog.title.required}")
        @Size(max = 255, message = "{validation.blog.title.max}")
        String title,

        @NotBlank(message = "{validation.blog.description.required}")
        String description,

        @NotBlank(message = "{validation.blog.content.required}")
        String content
) {
    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getContent() {
        return content;
    }
}
