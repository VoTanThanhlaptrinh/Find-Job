package com.job_web.recruiment.domain.repository;

import com.job_web.recruiment.api.dto.JobCardView;
import com.job_web.recruiment.domain.model.Job;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    long countByStatus(String status);

    @Query("""
        SELECT j FROM Job j WHERE j.status = :status
    """)
    Page<Job> findByStatus(String status, Pageable pageable);

    @Query("""
        SELECT new com.job_web.recruiment.api.dto.JobCardView(
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

    @Query(value = """
                WITH cv_info AS (
                SELECT
                    cv.experience_embedding AS cv_exp_vec,
                    cv.skill_and_project_embedding AS cv_skill_vec,
                    r.yoe AS cv_yoe
                FROM cv_vectors cv
                JOIN resume r ON cv.id = r.id
                WHERE cv.id = :cvId
                LIMIT 1
            ),
            stage_1_retrieval AS (
                SELECT
                    jv.id AS job_id,
                    jv.skill_and_project_embedding,
                    (jv.experience_embedding <=> ci.cv_exp_vec) AS exp_dist,
                    ci.cv_skill_vec
                FROM cv_info ci
                CROSS JOIN LATERAL (
                    SELECT id, experience_embedding, skill_and_project_embedding
                    FROM jd_vectors
                    WHERE required_years_experience <= ci.cv_yoe
                    ORDER BY experience_embedding <=> ci.cv_exp_vec
                    LIMIT 200
                ) jv
            ),
            stage_2_rerank AS (
                SELECT
                    job_id,
                    exp_dist,
                    (skill_and_project_embedding <=> cv_skill_vec) AS skill_dist,
                    (exp_dist * 0.6 + (skill_and_project_embedding <=> cv_skill_vec) * 0.4) AS final_dist
                FROM stage_1_retrieval
                where (exp_dist * 0.6 + (skill_and_project_embedding <=> cv_skill_vec) * 0.4) <= 0.5
                ORDER BY final_dist ASC
                LIMIT 10
            )
            SELECT
                j.id,
                j.title,
                a.city as address, 
                j.salary,
                j.time
            FROM stage_2_rerank r
            JOIN job j ON r.job_id = j.id
            join address a on a.id = j.address_id
            ORDER BY r.final_dist ASC;
            """, nativeQuery = true)
    List<Tuple> matchJobs(long cvId);
}
