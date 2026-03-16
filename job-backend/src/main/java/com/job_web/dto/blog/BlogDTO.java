package com.job_web.dto.blog;

import com.job_web.models.Blog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class BlogDTO {
    @NotBlank(message = "Tiêu đề không được rỗng")
    @Size(max = 255, message = "Tiêu đề tối đa 255 ký tự")
    private String title;

    @NotBlank(message = "Mô tả không được rỗng")
    private String description;

    @NotBlank(message = "Nội dung không được rỗng")
    private String content;

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
}





