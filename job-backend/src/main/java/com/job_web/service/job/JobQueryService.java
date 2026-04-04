package com.job_web.service.job;

import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobQueryService {
    Page<JobCardView> getListJobNewest(int page, int amount);

    Integer getAmount();

    List<AddressJobCount> getAddressJobCount();

    Page<JobCardView> getListJobByAddress(String address, int page, int amount);

    Page<JobCardView> filterBetterSalaryAndHasAddressAndInTimes(int pageIndex, int pageSize, int min, int max, List<String> cities, List<String> times, String title);

    Page<JobResponse> getHirerJobPost(int pageIndex, int pageSize, String email);

    Long countHirerJobPost(String email);

    Page<JobCardView> listJobUserApplied(Pageable pageable, String email);
}
