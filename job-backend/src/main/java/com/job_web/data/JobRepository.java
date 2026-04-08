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

    @Query(value = """
        WITH cv_info AS (
            SELECT 
                cv.skills_projects_embedding AS cv_skill_vec,
                cv.experience_embedding AS cv_exp_vec,
                r.title AS cv_title,
                a.city AS cv_city,
                r.yoe AS cv_yoe
            FROM cv_vectors cv
            JOIN resume r ON cv.id = r.id
            LEFT JOIN address a ON r.address_id = a.id
            WHERE cv.id = :cvId
        ),
        job_matches AS (
            SELECT 
                j.id,
                j.title,
                a.city AS address,
                j.salary,
                j.time,
                (
                    (0.4 * (1 - (jv.skill_and_project_embedding <=> ci.cv_skill_vec))) + 
                    (0.6 * (1 - (jv.experience_embedding <=> ci.cv_exp_vec)))
                ) AS base_score,
                CASE WHEN LOWER(a.city) = LOWER(ci.cv_city) THEN 0.10 ELSE 0 END AS city_bonus,
                (similarity(LOWER(j.title), LOWER(ci.cv_title)) * 0.15) AS title_bonus
            FROM jd_vectors jv
            CROSS JOIN cv_info ci
            JOIN job j ON jv.id = j.id
            LEFT JOIN address a ON j.address_id = a.id
            WHERE jv.required_years_experience <= ci.cv_yoe
        )
        SELECT 
            id,
            title,
            address,
            salary,
            time
        FROM job_matches
        ORDER BY (base_score + city_bonus + title_bonus) DESC
        LIMIT 10
        """, nativeQuery = true)
    List<JobCardView> matchJobs(long cvId);
}
