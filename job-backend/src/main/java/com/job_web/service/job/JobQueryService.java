package com.job_web.service.job;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobApply;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.security.Principal;
import java.util.List;

public interface JobQueryService {
    ApiResponse<Page<JobCardView>> getListJobNewest(int page, int amount);

    ApiResponse<Integer> getAmount();

    ApiResponse<List<AddressJobCount>> getAddressJobCount();

    ApiResponse<Page<JobCardView>> getListJobByAddress(String address, int page, int amount);

    ApiResponse<Page<JobCardView>> filterBetterSalaryAndHasAddressAndInTimes(int pageIndex, int pageSize, int min, int max, List<String> cities, List<String> times, String title);

    ApiResponse<Page<JobResponse>> getHirerJobPost(int pageIndex, int pageSize, Principal principal);

    ApiResponse<Long> countHirerJobPost(Principal principal);

    ApiResponse<Page<JobCardView>> listJobUserApplied(Pageable pageable, Principal principal);


}
