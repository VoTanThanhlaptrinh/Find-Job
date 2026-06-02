package com.job_web.content.domain.repository;

import com.job_web.content.domain.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends PagingAndSortingRepository<Comment, Long>, JpaRepository<Comment,Long> {

}


