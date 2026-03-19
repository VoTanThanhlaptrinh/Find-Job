package com.job_web.service.job;

import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.HirerJobPostView;
import com.job_web.dto.job.JobApply;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobDetailView;
import com.job_web.dto.job.PagedPayload;

import com.job_web.dto.common.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.util.List;

public interface JobService {
	ApiResponse<PagedPayload<JobCardView>> getListJobNewest(int page,int amount);
	ApiResponse<JobDetailView> getJobDetailById(String id);
	ApiResponse<Boolean> checkExistJob(String id);

	ApiResponse<Integer> getAmount();

	ApiResponse<List<AddressJobCount>> getAddressJobCount();

	ApiResponse<PagedPayload<JobCardView>> getListJobByAddress(String address, int page,int amount);

	ApiResponse<PagedPayload<JobCardView>> filterBetterSalaryAndHasAddressAndInTimes(final int pageIndex, final int pageSize,final int min,final int max, final List<String> address, final List<String> times);

	ApiResponse<String> createJob(JobDTO jobDTO, Principal principal);

	ApiResponse<String> updateJob(long id, JobDTO jobDTO, Principal principal);

	ApiResponse<String> deleteJob(long id, Principal principal);

    ApiResponse<PagedPayload<HirerJobPostView>> getHirerJobPost(int pageIndex, int pageSize, Principal principal);

	ApiResponse<Long> countHirerJobPost(Principal principal);

    ApiResponse<Page<JobApply>> listJobUserApplied(Pageable pageable, Principal principal);

}



