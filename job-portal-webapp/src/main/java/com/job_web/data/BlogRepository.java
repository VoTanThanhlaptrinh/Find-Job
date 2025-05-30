package com.job_web.data;

import org.springframework.data.repository.PagingAndSortingRepository;

import com.job_web.models.Blog;

public interface BlogRepository extends PagingAndSortingRepository<Blog, Long> {
}
