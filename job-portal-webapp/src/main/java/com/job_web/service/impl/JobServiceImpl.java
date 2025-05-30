package com.job_web.service.impl;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.job_web.data.JobRepository;
import com.job_web.dto.ApiResponse;
import com.job_web.models.Job;
import com.job_web.service.JobService;

import lombok.AllArgsConstructor;
@Service
@AllArgsConstructor
public class JobServiceImpl implements JobService {
	private final JobRepository jobRepository;

	@Override
	public ApiResponse<Page<Job>> getListJobNewest(int page, int amount) {
		PageRequest pageable = PageRequest.of(page, amount, Sort.by("createDate").descending());

		return new ApiResponse<Page<Job>>("success", jobRepository.findAll(pageable), 200);
	}

	@Override
	public ApiResponse<Job> getJobDetailById(String id) {
		try {
			long jobId = Long.valueOf(id);
			Optional<Job> job = jobRepository.findById(jobId);
			if (job.isPresent())
				return new ApiResponse<Job>("success", job.get(), 200);
			return new ApiResponse<Job>("Id not found", null, 404);
		} catch (NumberFormatException e) {
			return new ApiResponse<Job>("Id is not number", null, 404);
		}
	}

	@Override
	public ApiResponse<Boolean> checkExistJob(String id) {
		try {
			long jobId = Long.valueOf(id);
			Optional<Job> job = jobRepository.findById(jobId);
				return new ApiResponse<Boolean>("success", job.isPresent(), 200);
		} catch (NumberFormatException e) {
			return new ApiResponse<Boolean>("Id is not number", false, 404);
		}
	}

}
