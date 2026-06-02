package com.job_web.admin.application;

import com.job_web.admin.api.dto.seeker.JobSeekerListItem;
import com.job_web.admin.api.dto.seeker.JobSeekerMetricsResponse;
import com.job_web.admin.api.dto.seeker.JobSeekerRequest;
import com.job_web.admin.api.dto.seeker.RegionDistributionResponse;
import com.job_web.shared.domain.model.PageResponse;

public interface AdminJobSeekerService {
    JobSeekerMetricsResponse getJobSeekerMetrics();

    PageResponse<JobSeekerListItem> getJobSeekers(int page, int pageSize, String search, String resumeStatus);

    void createJobSeeker(JobSeekerRequest request);

    RegionDistributionResponse getRegionDistribution();
}
