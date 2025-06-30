package com.job_web.data;

import org.springframework.data.repository.CrudRepository;

import com.job_web.models.Comment;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends PagingAndSortingRepository<Comment, Long> {

}
