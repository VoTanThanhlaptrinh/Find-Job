package com.job_web.data;

import org.springframework.data.repository.CrudRepository;

import com.job_web.models.Apply;

public interface ApplyRepository extends CrudRepository<Apply, Long> {
	
}
