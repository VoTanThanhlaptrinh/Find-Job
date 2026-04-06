package com.job_web.dto.blog;

import com.job_web.models.Blog;
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
    public Blog toBlog() {
        Blog blog = new Blog();
        applyTo(blog);
        return blog;
    }

    public void applyTo(Blog blog) {
        blog.setTitle(title);
        blog.setDescription(description);
        blog.setContent(content);
    }

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
