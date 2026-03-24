package com.job_web.data;

import com.job_web.models.Like;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface LikeRepository extends CrudRepository<Like, Long> {
   @Query(value = """
           select *
           from user_like
           where user_id = ?1 and blog_id = ?2
           order by id desc
           limit 1
           """, nativeQuery = true)
   Optional<Like> findLatestByUserIdAndBlogId(long userId, long blogId);
}


