package com.job_web.mapper;

import com.job_web.dto.blog.BlogDTO;
import com.job_web.models.Blog;
import org.springframework.stereotype.Component;

@Component
public class BlogMapper {

    public Blog toBlog(BlogDTO blogDTO) {
        Blog blog = new Blog();
        applyTo(blogDTO, blog);
        return blog;
    }

    public void applyTo(BlogDTO blogDTO, Blog blog) {
        blog.setTitle(blogDTO.title());
        blog.setDescription(blogDTO.description());
        blog.setContent(blogDTO.content());
    }
}
