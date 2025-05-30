package com.job_web.service;

import org.springframework.data.domain.Page;

import com.job_web.dto.ApiResponse;
import com.job_web.models.Job;

public interface JobService {
	ApiResponse<Page<Job>> getListJobNewest(int page,int amount);
	ApiResponse<Job> getJobDetailById(String id);
	ApiResponse<Boolean> checkExistJob(String id);
}
