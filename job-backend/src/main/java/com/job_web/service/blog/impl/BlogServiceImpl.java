package com.job_web.service.blog.impl;

import com.job_web.data.BlogRepository;
import com.job_web.data.CommentRepository;
import com.job_web.data.LikeRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.blog.BlogDTO;
import com.job_web.models.Blog;
import com.job_web.models.Comment;
import com.job_web.models.Like;
import com.job_web.models.User;
import com.job_web.service.blog.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final BlogRepository blogRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;

    @Override
    public ApiResponse<String> postBlog(BlogDTO blogDTO, User user) {
        Blog blog = blogDTO.toBlog();
        blog.setAuthor(user);
        blogRepository.save(blog);
        return new ApiResponse<>("success", null, HttpStatus.CREATED.value());
    }

    @Override
    public ApiResponse<String> updateBlog(long id, BlogDTO blogDTO, User user) {
        Optional<Blog> blogOpt = blogRepository.findBlogById(id);
        if (blogOpt.isEmpty()) {
            return new ApiResponse<>("không tìm thấy blog", null, HttpStatus.NOT_FOUND.value());
        }
        Blog blog = blogOpt.get();
        if (blog.getAuthor() != null && blog.getAuthor().getId() != user.getId()) {
            return new ApiResponse<>("Không có quyền chỉnh sửa", null, HttpStatus.FORBIDDEN.value());
        }
        blogDTO.applyTo(blog);
        blogRepository.save(blog);
        return new ApiResponse<>("success", null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> deleteBlog(long id) {
        Optional<Blog> blogOpt = blogRepository.findBlogById(id);
        if (blogOpt.isEmpty()) {
            return new ApiResponse<>("không tìm thấy blog", null, HttpStatus.NOT_FOUND.value());
        }
        Blog blog = blogOpt.get();
        blog.markDeleted();
        blogRepository.save(blog);
        return new ApiResponse<>("success", null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Page<Blog>> getBlogs(final int pageIndex, final int pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize);
        Page<Blog> blogs = blogRepository.findAll(pageable);
        return new ApiResponse<>("success",blogs, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Blog> getBlogById(long id) {
        return blogRepository.findBlogById(id).map(blog -> new ApiResponse<>("success",blog,HttpStatus.OK.value())).orElseGet(() ->  new ApiResponse<>("error",null,HttpStatus.NOT_FOUND.value()));
    }

    @Override
    public ApiResponse<String> comment(Comment comment, User user) {
        comment.setUser(user);
        comment.markActive();
        commentRepository.save(comment);
        return new ApiResponse<>("success", null, HttpStatus.CREATED.value());
    }

    @Override
    public ApiResponse<String> like(final long id, User user) {
        Optional<Blog> blog = blogRepository.findBlogById(id);

        if(blog.isEmpty()) {
            return new ApiResponse<>("không tìm thấy blog",null, HttpStatus.NOT_FOUND.value());
        }
        Optional<Like> like = likeRepository.findLatestByUserIdAndBlogId(user.getId(), blog.get().getId());
        like.ifPresentOrElse(
                l -> {
                    l.markActive();
                    likeRepository.save(l);
                },
                () -> {
                    Like like1 = new Like();
                    like1.setUser(user);
                    like1.setBlog(blog.get());
                    like1.markActive();
                    likeRepository.save(like1);
                }
        );
        return new ApiResponse<>("success",null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> unlike(long id, User user) {
        Optional<Blog> blog = blogRepository.findBlogById(id);

        if(blog.isEmpty()) {
            return new ApiResponse<>("không tìm thấy blog",null, HttpStatus.NOT_FOUND.value());
        }
        Optional<Like> like = likeRepository.findLatestByUserIdAndBlogId(user.getId(), blog.get().getId());
        like.ifPresent(l -> {
            l.markDeleted();
            likeRepository.save(l);
        });
        return new ApiResponse<>("success",null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Page<Comment>> getComments(int pageIndex, int pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createDate").descending());
        Page<Comment> comments = commentRepository.findAll(pageable);
        return new ApiResponse<>("success",comments, HttpStatus.OK.value());
    }

}
