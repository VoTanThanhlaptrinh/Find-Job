package com.job_web.service.blog;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.blog.BlogDTO;
import com.job_web.models.Blog;
import com.job_web.models.Comment;
import org.springframework.data.domain.Page;

public interface BlogService {
    ApiResponse<String> postBlog(BlogDTO blog);
    ApiResponse<String> updateBlog(long id, BlogDTO blog);
    ApiResponse<String> deleteBlog(long id);
    ApiResponse<Page<Blog>> getBlogs(int pageIndex, int pageSize);
    ApiResponse<Blog> getBlogById(long id);
    ApiResponse<String> comment(Comment comment);
    ApiResponse<String> like(long id);
    ApiResponse<String> unlike(long id);
    ApiResponse<Page<Comment>> getComments(int pageIndex, int pageSize);
}



