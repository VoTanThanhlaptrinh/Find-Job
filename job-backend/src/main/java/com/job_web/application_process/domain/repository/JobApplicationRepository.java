package com.job_web.application_process.domain.repository;

import com.job_web.application_process.api.dto.CandidateDTO;
import com.job_web.application_process.domain.model.JobApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    @Query(value = """
            select a.id
            from jobApplication a
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
            from jobApplication a
            join job j on a.job_id = j.id 
            join users u on a.user_id = u.id
            join resume r on a.resume_id = r.id 
            where j.id = ?1
            """, nativeQuery = true)
    Page<CandidateDTO> getAllCandidateAppliedJob(long jobId, Pageable pageable);
}
