package com.nlu.content.domain.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nlu.content.domain.model.Blog;
import com.nlu.content.domain.vo.BlogStatus;

import java.util.List;
import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, Long> {
    Optional<Blog> findBlogById(long id);

    Page<Blog> findByStatusAndTitleContainingIgnoreCase(Pageable pageRequest, BlogStatus status, String title);

    Page<Blog> findByTitleContainingIgnoreCaseAndAuthor_Id(Pageable pageRequest, String title, long userId);
}
