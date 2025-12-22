package com.job_web.service.impl;

import com.job_web.data.BlogRepository;
import com.job_web.data.CommentRepository;
import com.job_web.data.LikeRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.ApiResponse;
import com.job_web.dto.BlogDTO;
import com.job_web.models.Blog;
import com.job_web.models.Comment;
import com.job_web.models.Like;
import com.job_web.models.User;
import com.job_web.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final BlogRepository blogRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;
    @Override
    public ApiResponse<String> postBlog(BlogDTO blogDTO) {
        Blog blog = blogDTO.toBlog();
        Optional<User> user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
        if(user.isEmpty()) {
            return new ApiResponse<>("không tìm thấy user",null, HttpStatus.NOT_FOUND.value());
        }
        blog.setAuthor(user.get());
        blogRepository.save(blog);
        return new ApiResponse<>("success",null, HttpStatus.CREATED.value());
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
    public ApiResponse<String> comment(Comment comment) {
        Optional<User> user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
        if(user.isEmpty()) {
            return new ApiResponse<>("không tìm thấy user",null, HttpStatus.NOT_FOUND.value());
        }
        comment.setUser(user.get());
        commentRepository.save(comment);
        return new ApiResponse<>("success",null, HttpStatus.CREATED.value());
    }

    @Override
    public ApiResponse<String> like(final long id) {
        Optional<User> user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
        Optional<Blog> blog = blogRepository.findBlogById(id);

        if(user.isEmpty()) {
            return new ApiResponse<>("không tìm thấy user",null, HttpStatus.NOT_FOUND.value());
        }
        if(blog.isEmpty()) {
            return new ApiResponse<>("không tìm thấy blog",null, HttpStatus.NOT_FOUND.value());
        }
        Optional<Like> like = likeRepository.findLikeByUserAndBlog(user.get(),blog.get());
        like.ifPresentOrElse(
                l -> {
                    l.setStatus("like");
                    likeRepository.save(l);
                },
                () -> {
                    Like like1 = new Like();
                    like1.setUser(user.get());
                    like1.setBlog(blog.get());
                    like1.setStatus("like");
                    likeRepository.save(like1);
                }
        );
        return new ApiResponse<>("success",null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> unlike(long id) {
        Optional<User> user = userRepository.findByEmail(SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString());
        Optional<Blog> blog = blogRepository.findBlogById(id);

        if(user.isEmpty()) {
            return new ApiResponse<>("không tìm thấy user",null, HttpStatus.NOT_FOUND.value());
        }
        if(blog.isEmpty()) {
            return new ApiResponse<>("không tìm thấy blog",null, HttpStatus.NOT_FOUND.value());
        }
        Optional<Like> like = likeRepository.findLikeByUserAndBlog(user.get(),blog.get());
        like.ifPresentOrElse(
                l -> {
                    l.setStatus("unlike");
                    likeRepository.save(l);
                },
                () -> {
                    Like like1 = new Like();
                    like1.setUser(user.get());
                    like1.setBlog(blog.get());
                    like1.setStatus("unlike");
                    likeRepository.save(like1);
                }
        );
        return new ApiResponse<>("success",null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Page<Comment>> getComments(int pageIndex, int pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createDate").descending());
        Page<Comment> comments = commentRepository.findAll(pageable);
        return new ApiResponse<>("success",comments, HttpStatus.OK.value());
    }

}
