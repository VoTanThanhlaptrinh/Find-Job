package com.job_web.data;

import java.util.List;

import com.job_web.dto.AddressJobCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.job_web.models.Job;

public interface JobRepository extends PagingAndSortingRepository<Job, Long>, JpaSpecificationExecutor<Job> {
	@Query(value = "select * from job where title like CONCAT('%', ?1, '%')	", nativeQuery = true)
	List<Job> findByTitle(String title);

	@Query(value = "select address as address, count(address) as count from job group by address",nativeQuery = true)
	List<AddressJobCount> findAddressJobCount();

	@Query(value = "select * from job where job.time = ?1",nativeQuery = true)
	Page<Job> findByTime(String time, Pageable pageable);
}
