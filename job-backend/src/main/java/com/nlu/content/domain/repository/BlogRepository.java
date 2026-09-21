package com.nlu.content.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nlu.content.domain.model.Blog;

import java.util.List;
import java.util.Optional;

public interface BlogRepository extends JpaRepository<Blog, Long> {
    Optional<Blog> findBlogById(long id);

    List<Blog> id(long id);
}


