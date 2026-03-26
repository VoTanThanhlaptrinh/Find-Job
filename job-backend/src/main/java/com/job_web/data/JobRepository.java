package com.job_web.data;

import com.job_web.dto.job.JobCardView;
import com.job_web.models.Job;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    @Query("""
        SELECT new com.job_web.dto.job.JobCardView(
            j.id,
            j.title,
            j.address.city,
            j.salary,
            j.time
        )
        FROM Job j
        WHERE j.createDate < :createDateBefore
        AND j.status = :status
    """)
    List<JobCardView> findJobs(LocalDateTime createDateBefore, String status, Pageable pageable);
}
