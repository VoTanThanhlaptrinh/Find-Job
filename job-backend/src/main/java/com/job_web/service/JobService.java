package com.job_web.service;

import com.job_web.dto.AddressJobCount;
import com.job_web.dto.JobResponse;
import org.springframework.data.domain.Page;

import com.job_web.dto.ApiResponse;
import com.job_web.models.Job;

import java.security.Principal;
import java.util.List;

public interface JobService {
	ApiResponse<Page<Job>> getListJobNewest(int page,int amount);
	ApiResponse<Job> getJobDetailById(String id);
	ApiResponse<Boolean> checkExistJob(String id);

	ApiResponse<Integer> getAmount();

	ApiResponse<List<AddressJobCount>> getAddressJobCount();

	ApiResponse<Page<Job>> getListJobByAddress(String address, int page,int amount);

	ApiResponse<Page<Job>> filterBetterSalaryAndHasAddressAndInTimes(final int pageIndex, final int pageSize,final int min,final int max, final List<String> address, final List<String> times);

	ApiResponse<String> saveJob(Job job, Principal principal);

    ApiResponse<Page<JobResponse>> getHirerJobPost(int pageIndex, int pageSize, Principal principal);

	ApiResponse<Long> countHirerJobPost(Principal principal);
}
