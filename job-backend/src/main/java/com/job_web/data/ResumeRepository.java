package com.job_web.data;

import com.job_web.models.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    @Query(value = """
            select count(*)
            from resume r
            join user u on r.user_id = u.id and u.status = 'ACTIVE'
            where u.email = ?1
              and r.status = 'ACTIVE'
            """, nativeQuery = true)
    long countActiveByUserEmail(String email);

    @Query(value = """
            select count(*)
            from resume r
            join user u on r.user_id = u.id and u.status = 'ACTIVE'
            where r.id = ?1
              and u.email = ?2
              and r.status = 'ACTIVE'
            """, nativeQuery = true)
    long countOwnedByUser(long resumeId, String email);
}


