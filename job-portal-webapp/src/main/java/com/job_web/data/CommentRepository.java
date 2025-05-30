package com.job_web.data;

import org.springframework.data.repository.CrudRepository;

import com.job_web.models.Comment;

public interface CommentRepository extends CrudRepository<Comment, Long> {

}
