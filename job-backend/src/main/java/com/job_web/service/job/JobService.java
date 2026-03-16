package com.job_web.service.job;

import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobResponse;
import com.job_web.dto.job.JobApply;
import com.job_web.dto.job.JobDTO;
import org.springframework.data.domain.Page;

import com.job_web.dto.common.ApiResponse;
import com.job_web.models.Job;
import org.springframework.data.domain.Pageable;

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

	ApiResponse<String> createJob(JobDTO jobDTO, Principal principal);

	ApiResponse<String> updateJob(long id, JobDTO jobDTO, Principal principal);

	ApiResponse<String> deleteJob(long id, Principal principal);

    ApiResponse<Page<JobResponse>> getHirerJobPost(int pageIndex, int pageSize, Principal principal);

	ApiResponse<Long> countHirerJobPost(Principal principal);

    ApiResponse<Page<JobApply>> listJobUserApplied(Pageable pageable, Principal principal);

}



