package com.job_web.content.application;


import com.job_web.content.api.dto.BlogDTO;
import com.job_web.content.domain.model.Blog;
import com.job_web.content.domain.model.Comment;
import com.job_web.identity.domain.model.User;
import org.springframework.data.domain.Page;

public interface BlogService {
    String postBlog(BlogDTO blog, User user);
    String updateBlog(long id, BlogDTO blog, User user);
    String deleteBlog(long id);
    Page<Blog> getBlogs(int pageIndex, int pageSize);
    Blog getBlogById(long id);
    String comment(Comment comment, User user);
    String like(long id, User user);
    String unlike(long id, User user);
    Page<Comment> getComments(int pageIndex, int pageSize);
}
