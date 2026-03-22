package com.job_web.data;

import java.util.List;

import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobApply;
import com.job_web.dto.job.JobResponse;
import com.job_web.models.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface JobRepository extends PagingAndSortingRepository<Job, Long>, JpaSpecificationExecutor<Job>, JpaRepository<Job, Long> {
    @Query(value = "select * from job where status = 'ACTIVE' and title like CONCAT('%', ?1, '%')", nativeQuery = true)
    List<Job> findByTitle(String title);

    @Query(value = """
            select concat_ws(', ', a.street, a.district, a.city) as address,
                   count(j.id) as count
            from job j
            left join address a on j.address_id = a.id
            where j.status = 'ACTIVE'
              and (a.id is null or a.status = 'ACTIVE')
            group by a.street, a.district, a.city
            """, nativeQuery = true)
    List<AddressJobCount> findAddressJobCount();

    @Query(value = "select * from job where status = 'ACTIVE' and job.time = ?1", nativeQuery = true)
    Page<Job> findByTime(String time, Pageable pageable);

    @Query(
            value = """
                    SELECT j.id AS id,
                           j.title AS title,
                           j.description AS description,
                           CONCAT_WS(', ', a.street, a.district, a.city) AS address,
                           j.salary AS salary,
                           j.time AS time,
                           j.create_date AS create_date,
                           COUNT(ap.id) AS applies
                    FROM job j
                    LEFT JOIN address a ON j.address_id = a.id
                    LEFT JOIN hirer h ON j.hirer_id = h.id
                    LEFT JOIN user u ON h.user_id = u.id
                    LEFT JOIN apply ap ON ap.job_id = j.id AND ap.status = 'ACTIVE'
                    WHERE u.email = ?1
                      AND j.status = 'ACTIVE'
                      AND h.status = 'ACTIVE'
                      AND u.status = 'ACTIVE'
                      AND (a.id IS NULL OR a.status = 'ACTIVE')
                    GROUP BY j.id, j.title, j.description, a.street, a.district, a.city, j.salary, j.time, j.create_date
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM job j
                    LEFT JOIN hirer h ON j.hirer_id = h.id
                    LEFT JOIN user u ON h.user_id = u.id
                    WHERE u.email = ?1
                      AND j.status = 'ACTIVE'
                      AND h.status = 'ACTIVE'
                      AND u.status = 'ACTIVE'
                    """,
            nativeQuery = true
    )
    Page<JobResponse> getJobPostOfHirer(String username, Pageable pageable);

    @Query(value = """
            select count(*)
            from job j
            left join hirer h on j.hirer_id = h.id
            left join user u on h.user_id = u.id
            where u.email = ?1
              and j.status = 'ACTIVE'
              and h.status = 'ACTIVE'
              and u.status = 'ACTIVE'
            """, nativeQuery = true)
    long getHirerJobCount(String email);

    @Query(value = """
            SELECT j.id AS id,
                   j.title AS title,
                   j.description AS description,
                   CONCAT_WS(', ', a.street, a.district, a.city) AS address,
                   j.salary AS salary,
                   j.time AS time
            FROM user u
            JOIN apply ap ON u.id = ap.user_id AND ap.status = 'ACTIVE'
            JOIN job j ON j.id = ap.job_id AND j.status = 'ACTIVE'
            LEFT JOIN address a ON j.address_id = a.id
            WHERE u.email = ?1
              AND u.status = 'ACTIVE'
              AND (a.id IS NULL OR a.status = 'ACTIVE')
            ORDER BY ap.apply_date DESC
            """, nativeQuery = true)
    Page<JobApply> listJobUserApplies(String username, Pageable pageable);
}
