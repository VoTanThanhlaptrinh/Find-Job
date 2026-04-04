package com.job_web.service.job.impl;

import com.job_web.data.queryDSL.JobQuery;
import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobResponse;
import com.job_web.service.job.JobQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobQueryServiceImpl implements JobQueryService {
    private final JobQuery jobQuery;

    @Override
    public Page<JobCardView> getListJobNewest(int page, int amount) {
        return jobQuery.getListJobNewest(page, amount);
    }

    @Override
    public Integer getAmount() {
        return jobQuery.getAmount();
    }

    @Override
    public List<AddressJobCount> getAddressJobCount() {
        return jobQuery.getAddressJobCount();
    }

    @Override
    public Page<JobCardView> getListJobByAddress(String city, int page, int amount) {
        return jobQuery.getListJobByAddress(city, page, amount);
    }

    @Override
    public Page<JobCardView> filterBetterSalaryAndHasAddressAndInTimes(int pageIndex, int pageSize, int min, int max, List<String> cities, List<String> times, String title) {
        return jobQuery.filterBetterSalaryAndHasAddressAndInTimes(pageIndex, pageSize, min, max, cities, times, title);
    }

    @Override
    public Page<JobResponse> getHirerJobPost(int pageIndex, int pageSize, String email) {
        return jobQuery.getHirerJobPost(pageIndex, pageSize, email);
    }

    @Override
    public Long countHirerJobPost(String email) {
        return jobQuery.countHirerJobPost(email);
    }

    @Override
    public Page<JobCardView> listJobUserApplied(Pageable pageable, String email) {
        return jobQuery.listJobUserApplied(pageable, email);
    }
}
