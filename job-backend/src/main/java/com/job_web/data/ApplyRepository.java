package com.job_web.data;

import com.job_web.dto.application.CandidateDTO;
import com.job_web.models.Apply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplyRepository extends JpaRepository<Apply, Long> {
    @Query(value = """
            select a.id
            from apply a
            join users u on a.user_id = u.id\s
                    join job j on j.id = a.job_id
                    where u.email = ?1
                      and j.id = ?2
                    limit 1
            """, nativeQuery = true)
    Optional<Long> findByJobAndUser(String email, long jobId);

    @Query(value = """
            select u.full_name as full_name,
                   u.email as email,
                   r.file_name as file_name,
                   a.apply_date
            from apply a
            join job j on a.job_id = j.id 
            join users u on a.user_id = u.id
            join resume r on a.resume_id = r.id 
            where j.id = ?1
            """, nativeQuery = true)
    Page<CandidateDTO> getAllCandidateAppliedJob(long jobId, Pageable pageable);
}
