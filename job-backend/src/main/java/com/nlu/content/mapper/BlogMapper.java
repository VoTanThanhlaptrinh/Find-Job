package com.nlu.content.mapper;

import com.nlu.content.api.dto.BlogDTO;
import com.nlu.content.domain.model.Blog;
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
