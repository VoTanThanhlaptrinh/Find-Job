package com.nlu.content.application;

import org.springframework.data.domain.Page;

import com.nlu.content.api.dto.BlogDTO;
import com.nlu.content.api.dto.BlogDetail;
import com.nlu.content.api.dto.BlogResponse;
import com.nlu.content.domain.model.Comment;
import com.nlu.identity.domain.model.User;

public interface BlogService {
    String postBlog(BlogDTO blog, User user);

    String updateBlog(long id, BlogDTO blog, User user);

    String deleteBlog(long id, User user);

    Page<BlogResponse> getBlogs(int pageIndex, int pageSize, String title, String dateOrder);

    Page<BlogResponse> getMyBlogs(int pageIndex, int pageSize, String title, String dateOrder, User user);

    BlogDetail getBlogById(long id);

    String comment(Comment comment, User user);

    String like(long id, User user);

    String unlike(long id, User user);

    Page<Comment> getComments(int pageIndex, int pageSize);
}
