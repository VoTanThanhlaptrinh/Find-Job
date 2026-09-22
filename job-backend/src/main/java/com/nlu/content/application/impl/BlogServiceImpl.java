package com.nlu.content.application.impl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.nlu.content.api.dto.BlogDTO;
import com.nlu.content.api.dto.BlogDetail;
import com.nlu.content.api.dto.BlogResponse;
import com.nlu.content.application.BlogService;
import com.nlu.content.domain.model.Blog;
import com.nlu.content.domain.model.Comment;
import com.nlu.content.domain.model.Like;
import com.nlu.content.domain.repository.BlogRepository;
import com.nlu.content.domain.repository.CommentRepository;
import com.nlu.content.domain.repository.LikeRepository;
import com.nlu.content.domain.vo.BlogStatus;
import com.nlu.content.mapper.BlogMapper;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.utils.MessageUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {
    private final BlogRepository blogRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final BlogMapper blogMapper;

    @Override
    public String postBlog(BlogDTO blogDTO, User user) {
        Blog blog = blogMapper.toBlog(blogDTO);
        blog.setAuthor(user);
        blogRepository.save(blog);
        return MessageUtils.getMessage("blog.create.success");
    }

    @Override
    public String updateBlog(long id, BlogDTO blogDTO, User user) {
        Optional<Blog> blogOpt = blogRepository.findBlogById(id);
        if (blogOpt.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("blog.not_found"));
        }
        Blog blog = blogOpt.get();
        if (blog.getAuthor() != null && blog.getAuthor().getId() != user.getId()) {
            throw new ForbiddenException(MessageUtils.getMessage("blog.edit.forbidden"));
        }
        blogMapper.applyTo(blogDTO, blog);
        blogRepository.save(blog);
        return MessageUtils.getMessage("blog.update.success");
    }

    @Override
    public String deleteBlog(long id, User user) {
        Optional<Blog> blogOpt = blogRepository.findBlogById(id);
        if (blogOpt.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("blog.not_found"));
        }
        Blog blog = blogOpt.get();
        if (blog.getAuthor() != null && blog.getAuthor().getId() != user.getId()) {
            throw new ForbiddenException(MessageUtils.getMessage("blog.delete.forbidden"));
        }
        blog.markDeleted();
        blogRepository.save(blog);
        return MessageUtils.getMessage("blog.delete.success");
    }

    @Override
    public Page<BlogResponse> getBlogs(int pageIndex, int pageSize, String title, String dateOrder) {
        Sort s = Sort.by("createdAt").descending();
        if ("asc".equalsIgnoreCase(dateOrder)) {
            s = Sort.by("createdAt").ascending();
        }
        Pageable pageable = PageRequest.of(pageIndex, pageSize, s);

        Page<BlogResponse> blogs = blogRepository
                .findByStatusAndTitleContainingIgnoreCase(pageable, BlogStatus.PUBLIC, title)
                .map(b -> new BlogResponse(b.getId(), b.getTitle(), b.getDescription(), b.getAmountLike(),
                        b.getAuthor().getEmail(), b.getTime()));
        return blogs;
    }

    @Override
    public Page<BlogResponse> getMyBlogs(int pageIndex, int pageSize, String title, String dateOrder, User user) {
        Sort s = Sort.by("createdAt").descending();
        if ("asc".equalsIgnoreCase(dateOrder)) {
            s = Sort.by("createdAt").ascending();
        }
        Pageable pageable = PageRequest.of(pageIndex, pageSize, s);

        Page<BlogResponse> blogs = blogRepository
                .findByTitleContainingIgnoreCaseAndAuthor_Id(pageable, title, user.getId())
                .map(b -> new BlogResponse(b.getId(), b.getTitle(), b.getDescription(), b.getAmountLike(),
                        b.getAuthor().getEmail(), b.getTime()));
        return blogs;
    }

    @Override
    public BlogDetail getBlogById(long id) {
        return blogRepository.findBlogById(id)
                .map(b -> new BlogDetail(b.getId(), b.getTitle(), b.getDescription(), b.getContent(), b.getAmountLike(),
                        b.getAuthor().getEmail(), b.getTime()))
                .orElseThrow(() -> new ResourceNotFoundException(MessageUtils.getMessage("blog.not_found")));
    }

    @Override
    public String comment(Comment comment, User user) {
        comment.setUser(user);
        comment.markActive();
        commentRepository.save(comment);
        return MessageUtils.getMessage("blog.comment.success");
    }

    @Override
    public String like(final long id, User user) {
        Optional<Blog> blog = blogRepository.findBlogById(id);

        if (blog.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("blog.not_found"));
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
                });
        return MessageUtils.getMessage("blog.like.success");
    }

    @Override
    public String unlike(long id, User user) {
        Optional<Blog> blog = blogRepository.findBlogById(id);

        if (blog.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("blog.not_found"));
        }
        Optional<Like> like = likeRepository.findLatestByUserIdAndBlogId(user.getId(), blog.get().getId());
        like.ifPresent(l -> {
            l.markDeleted();
            likeRepository.save(l);
        });
        return MessageUtils.getMessage("blog.unlike.success");
    }

    @Override
    public Page<Comment> getComments(int pageIndex, int pageSize) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createdAt").descending());
        Page<Comment> comments = commentRepository.findAll(pageable);
        return comments;
    }

}
