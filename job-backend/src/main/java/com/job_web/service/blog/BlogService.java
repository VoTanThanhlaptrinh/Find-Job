package com.job_web.service.blog;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.blog.BlogDTO;
import com.job_web.models.Blog;
import com.job_web.models.Comment;
import com.job_web.models.User;
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
