package com.job_web.data;

import com.job_web.dto.application.CandidateDTO;
import com.job_web.models.Apply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplyRepository extends CrudRepository<Apply, Long> {
    @Query(value = """
            select a.*
            from apply a
            join user u on a.user_id = u.id and u.status = 'ACTIVE'
            join job j on j.id = a.job_id and j.status = 'ACTIVE'
            where u.email = ?1
              and j.id = ?2
              and a.status = 'ACTIVE'
            limit 1
            """, nativeQuery = true)
    Optional<Apply> findByJobAndUser(String email, long jobId);

    @Query(value = """
            select u.full_name as full_name,
                   u.email as email,
                   r.file_name as file_name,
                   a.apply_date
            from apply a
            join job j on a.job_id = j.id and j.status = 'ACTIVE'
            join user u on a.user_id = u.id and u.status = 'ACTIVE'
            join resume r on a.resume_id = r.id and r.status = 'ACTIVE'
            where j.id = ?1
              and a.status = 'ACTIVE'
            """, nativeQuery = true)
    Page<CandidateDTO> getAllCandidateAppliedJob(long jobId, Pageable pageable);
}
