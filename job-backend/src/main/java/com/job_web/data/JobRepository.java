package com.job_web.data;

import java.util.List;

import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobResponse;
import com.job_web.dto.job.JobApply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.job_web.models.Job;

public interface JobRepository extends PagingAndSortingRepository<Job, Long>, JpaSpecificationExecutor<Job>, JpaRepository<Job,Long> {
	@Query(value = "select * from job where title like CONCAT('%', ?1, '%')	", nativeQuery = true)
	List<Job> findByTitle(String title);

	@Query(value = "select address as address, count(address) as count from job group by address",nativeQuery = true)
	List<AddressJobCount> findAddressJobCount();

	@Query(value = "select * from job where job.time = ?1",nativeQuery = true)
	Page<Job> findByTime(String time, Pageable pageable);

	@Query(
			value = """
    SELECT j.id AS id,
           j.title AS title,
           j.description AS description,
           j.address AS address,
           j.salary AS salary,
           j.time AS time,
           j.create_date AS create_date,
           COUNT(a.user_id) AS applies
    FROM job j
    LEFT JOIN hirer h ON j.hirer_id = h.id
    LEFT JOIN user u ON h.user_id = u.id
    LEFT JOIN apply a ON a.job_id = j.id
    WHERE u.email = ?1
    GROUP BY j.id, j.title, j.description, j.address, j.salary, j.time
  """,
			nativeQuery = true
	)
	Page<JobResponse> getJobPostOfHirer(String username, Pageable pageable);

	@Query(value =
			"""
			select count(*)
			from job j
			left join hirer h on j.hirer_id = h.id
			left join user u on h.user_id = u.id
			where u.email = ?1
						""", nativeQuery = true)
	long getHirerJobCount(String email);

    @Query(value = """
            SELECT 		 j.id AS id,
                         j.title AS title,
                         j.description AS description,
                         j.address AS address,
                         j.salary AS salary,
                         j.time AS time
                         from user as u
                         join apply as a on u.id = a.user_id
                         join job as j on j.id = a.job_id
                         where u.email = ?1
            			 order by a.apply_date desc
            """, nativeQuery = true)
    Page<JobApply> listJobUserApplies(String username, Pageable pageable);


}


