package com.nlu.applicationProcess.domain.repository;

import com.nlu.applicationProcess.domain.model.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    @Query(value = """
            select count(*)
            from resume r
            join users u on r.user_id = u.id 
            where u.email = ?1
              and r.record_status = 'ACTIVE'
              and u.record_status = 'ACTIVE'
            """, nativeQuery = true)
    long countActiveByUserEmail(String email);

    @Query(value = """
            select count(*)
            from resume r
            join users u on r.user_id = u.id 
            where r.id = ?1
              and u.email = ?2
              and r.record_status = 'ACTIVE'
              and u.record_status = 'ACTIVE'
            """, nativeQuery = true)
    long countOwnedByUser(long resumeId, String email);

    int countResumesByUser_Id(long id);
}


