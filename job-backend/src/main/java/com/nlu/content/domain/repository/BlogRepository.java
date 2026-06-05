package com.nlu.content.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.nlu.content.domain.model.Blog;

import java.util.List;
import java.util.Optional;

public interface BlogRepository extends PagingAndSortingRepository<Blog, Long>, JpaRepository<Blog, Long> {
    Optional<Blog> findBlogById(long id);

    List<Blog> id(long id);
}


