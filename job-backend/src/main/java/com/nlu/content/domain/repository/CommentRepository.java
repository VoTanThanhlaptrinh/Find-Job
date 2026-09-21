package com.nlu.content.domain.repository;

import com.nlu.content.domain.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

}


