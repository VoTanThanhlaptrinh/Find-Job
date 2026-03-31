package com.job_web.service.job.impl;

import com.job_web.data.queryDSL.JobQuery;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobApply;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobResponse;
import com.job_web.service.job.JobQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobQueryServiceImpl implements JobQueryService {
    private final JobQuery jobQuery;

    @Override
    public ApiResponse<Page<JobCardView>> getListJobNewest(int page, int amount) {
        return new ApiResponse<>("success", jobQuery.getListJobNewest(page, amount), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Integer> getAmount() {
        return new ApiResponse<>("success", jobQuery.getAmount(), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<List<AddressJobCount>> getAddressJobCount() {
        return new ApiResponse<>("success", jobQuery.getAddressJobCount(), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Page<JobCardView>> getListJobByAddress(String city, int page, int amount) {
        return new ApiResponse<>("success", jobQuery.getListJobByAddress(city, page, amount), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Page<JobCardView>> filterBetterSalaryAndHasAddressAndInTimes(int pageIndex, int pageSize, int min, int max, List<String> cities, List<String> times, String title) {
        return new ApiResponse<>("success", jobQuery.filterBetterSalaryAndHasAddressAndInTimes(pageIndex, pageSize, min, max, cities, times, title), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Page<JobResponse>> getHirerJobPost(int pageIndex, int pageSize, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Ban chua dang nhap", null, HttpStatus.UNAUTHORIZED.value());
        }

        return new ApiResponse<>("success", jobQuery.getHirerJobPost(pageIndex, pageSize, principal.getName()), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Long> countHirerJobPost(Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Ban chua dang nhap", null, HttpStatus.UNAUTHORIZED.value());
        }

        return new ApiResponse<>("success", jobQuery.countHirerJobPost(principal.getName()), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Page<JobCardView>> listJobUserApplied(Pageable pageable, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Ban chua dang nhap", null, HttpStatus.UNAUTHORIZED.value());
        }

        return new ApiResponse<>("success", jobQuery.listJobUserApplied(pageable, principal.getName()), HttpStatus.OK.value());
    }
}
