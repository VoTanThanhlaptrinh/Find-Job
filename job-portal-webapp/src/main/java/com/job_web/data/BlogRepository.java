package com.job_web.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.job_web.models.Blog;

import java.util.List;
import java.util.Optional;

public interface BlogRepository extends PagingAndSortingRepository<Blog, Long> {
    Optional<Blog> findBlogById(long id);

    List<Blog> id(long id);
}
