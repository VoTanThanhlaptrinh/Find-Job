package com.job_web.data;

import com.job_web.models.Blog;
import com.job_web.models.Like;
import com.job_web.models.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LikeRepository extends CrudRepository<Like, Long> {
   Optional<Like> findLikeByUserAndBlog(User user, Blog blog);
}
