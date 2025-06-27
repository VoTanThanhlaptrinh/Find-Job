package com.job_web.service;

import com.job_web.dto.ApiResponse;
import com.job_web.dto.BlogDTO;
import com.job_web.models.Blog;
import com.job_web.models.Comment;
import org.springframework.data.domain.Page;

import java.util.List;

public interface BlogService {
    ApiResponse<String> postBlog(BlogDTO blog);
    ApiResponse<Page<Blog>> getBlogs(int pageIndex, int pageSize);
    ApiResponse<Blog> getBlogById(long id);
    ApiResponse<String> comment(Comment comment);
    ApiResponse<String> like(long id);
    ApiResponse<String> unlike(long id);
    ApiResponse<Page<Comment>> getComments(int pageIndex, int pageSize);
}
