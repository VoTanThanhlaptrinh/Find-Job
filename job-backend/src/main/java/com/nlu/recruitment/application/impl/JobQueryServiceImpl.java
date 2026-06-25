package com.nlu.recruitment.application.impl;

import com.nlu.recruitment.domain.vo.EmploymentType;
import com.nlu.recruitment.infrastructure.query.JobQuery;
import com.nlu.recruitment.api.dto.AddressJobCount;
import com.nlu.recruitment.api.dto.JobCardView;
import com.nlu.recruitment.api.dto.JobResponse;
import com.nlu.recruitment.application.JobQueryService;
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
    public Page<JobCardView> getNewestJobs(int page, int amount) {
        return jobQuery.getListJobNewest(page, amount);
    }

    @Override
    public Integer countJobs() {
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
    public Page<JobCardView> findJobsBySalaryAddressAndEmploymentTypes(int pageIndex, int pageSize, int min, int max, List<String> cities, List<EmploymentType> times, String title, List<Long> categoryIds) {
        return jobQuery.findJobsBySalaryAddressAndEmploymentTypes(pageIndex, pageSize, min, max, cities, times, title, categoryIds);
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
