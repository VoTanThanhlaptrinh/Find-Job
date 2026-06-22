package com.nlu.recruitment.application;

import com.nlu.recruitment.domain.vo.EmploymentType;
import com.nlu.recruitment.api.dto.AddressJobCount;
import com.nlu.recruitment.api.dto.JobCardView;
import com.nlu.recruitment.api.dto.JobResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface JobQueryService {
    Page<JobCardView> getNewestJobs(int page, int amount);

    Integer countJobs();

    List<AddressJobCount> getAddressJobCount();

    Page<JobCardView> getListJobByAddress(String address, int page, int amount);

    Page<JobCardView> findJobsBySalaryAddressAndEmploymentTypes(int pageIndex, int pageSize, int min, int max, List<String> cities, List<EmploymentType> times, String title, List<Long> categoryIds);

    Page<JobResponse> getHirerJobPost(int pageIndex, int pageSize, String email);

    Long countHirerJobPost(String email);

    Page<JobCardView> listJobUserApplied(Pageable pageable, String email);
}
