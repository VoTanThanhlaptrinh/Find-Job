package com.nlu.admin.application;

import com.nlu.admin.api.dto.seeker.JobSeekerListItem;
import com.nlu.admin.api.dto.seeker.JobSeekerMetricsResponse;
import com.nlu.admin.api.dto.seeker.JobSeekerRequest;
import com.nlu.admin.api.dto.seeker.RegionDistributionResponse;
import com.nlu.shared.domain.model.PageResponse;

public interface AdminJobSeekerService {
    JobSeekerMetricsResponse getJobSeekerMetrics();

    PageResponse<JobSeekerListItem> getJobSeekers(int page, int pageSize, String search, String resumeStatus);

    void createJobSeeker(JobSeekerRequest request);

    RegionDistributionResponse getRegionDistribution();
}
