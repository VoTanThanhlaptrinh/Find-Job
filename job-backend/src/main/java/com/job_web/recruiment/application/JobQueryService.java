package com.job_web.recruiment.application;

import com.job_web.recruiment.domain.vo.EmploymentType;
import com.job_web.recruiment.api.dto.AddressJobCount;
import com.job_web.recruiment.api.dto.JobCardView;
import com.job_web.recruiment.api.dto.JobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobQueryService {
    Page<JobCardView> getListJobNewest(int page, int amount);

    Integer getAmount();

    List<AddressJobCount> getAddressJobCount();

    Page<JobCardView> getListJobByAddress(String address, int page, int amount);

    Page<JobCardView> filterBetterSalaryAndHasAddressAndInTimes(int pageIndex, int pageSize, int min, int max, List<String> cities, List<EmploymentType> times, String title);

    Page<JobResponse> getHirerJobPost(int pageIndex, int pageSize, String email);

    Long countHirerJobPost(String email);

    Page<JobCardView> listJobUserApplied(Pageable pageable, String email);
}
