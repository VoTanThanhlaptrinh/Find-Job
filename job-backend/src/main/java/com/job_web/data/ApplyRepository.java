package com.job_web.data;

import com.job_web.dto.application.CandidateDTO;
import com.job_web.models.Candidate;
import com.job_web.models.Job;
import com.job_web.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.job_web.models.Apply;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ApplyRepository extends CrudRepository<Apply, Long> {
    @Query(value = """
            select *
            from apply as a
            join user as u on a.user_id = u.id
            join job as j on j.id = a.job_id
            where u.email = ?1 and j.id = ?2
            limit 1
            """, nativeQuery = true)
    Optional<Apply> findByJobAndUser(String email, long jobId);

    @Query(value = """
                 select u.full_name as full_name,
                		u.email as email,
                        r.file_name as file_name,
                        a.apply_date
                 from apply as a
                 join job as j on a.job_id = j.id
                 join user as u on a.user_id = u.id
                 join resume as r on a.resume_id = r.id
                 where j.id = ?1;
            """, nativeQuery = true)
    Page<CandidateDTO> getAllCandidateAppliedJob(long jobId, Pageable pageable);
}


