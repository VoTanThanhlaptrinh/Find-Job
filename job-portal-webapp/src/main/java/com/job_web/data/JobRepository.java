package com.job_web.data;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.job_web.models.Job;

public interface JobRepository extends PagingAndSortingRepository<Job, Long> {
	@Query(value = "select * from job where title like CONCAT('%', ?1, '%')	", nativeQuery = true)
	List<Job> findByTitle(String title);
}
