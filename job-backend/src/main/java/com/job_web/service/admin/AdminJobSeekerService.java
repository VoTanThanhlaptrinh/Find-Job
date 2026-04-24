package com.job_web.service.admin;

import com.job_web.dto.admin.seeker.JobSeekerListItem;
import com.job_web.dto.admin.seeker.JobSeekerMetricsResponse;
import com.job_web.dto.admin.seeker.JobSeekerRequest;
import com.job_web.dto.admin.seeker.RegionDistributionResponse;
import com.job_web.dto.common.PageResponse;

public interface AdminJobSeekerService {
    JobSeekerMetricsResponse getJobSeekerMetrics();

    PageResponse<JobSeekerListItem> getJobSeekers(int page, int pageSize, String search, String resumeStatus);

    void createJobSeeker(JobSeekerRequest request);

    RegionDistributionResponse getRegionDistribution();
}
