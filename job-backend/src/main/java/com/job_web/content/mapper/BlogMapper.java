package com.job_web.content.mapper;

import com.job_web.content.api.dto.BlogDTO;
import com.job_web.content.domain.model.Blog;
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
