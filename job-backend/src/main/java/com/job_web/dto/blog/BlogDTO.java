package com.job_web.dto.blog;

import com.job_web.models.Blog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BlogDTO(
        @NotBlank(message = "TiÃªu Ä‘á» khÃ´ng Ä‘Æ°á»£c rá»—ng")
        @Size(max = 255, message = "TiÃªu Ä‘á» tá»‘i Ä‘a 255 kÃ½ tá»±")
        String title,

        @NotBlank(message = "MÃ´ táº£ khÃ´ng Ä‘Æ°á»£c rá»—ng")
        String description,

        @NotBlank(message = "Ná»™i dung khÃ´ng Ä‘Æ°á»£c rá»—ng")
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
