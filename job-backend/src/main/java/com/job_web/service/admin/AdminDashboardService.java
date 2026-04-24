package com.job_web.service.admin;

import com.job_web.dto.admin.dashboard.DashboardSummaryResponse;
import com.job_web.dto.admin.dashboard.JobDistributionResponse;
import com.job_web.dto.admin.dashboard.PendingJobItem;
import com.job_web.dto.admin.dashboard.RevenueTrendResponse;
import com.job_web.dto.common.PageResponse;

public interface AdminDashboardService {
    DashboardSummaryResponse getDashboardSummary();

    RevenueTrendResponse getRevenueTrend(String range);

    JobDistributionResponse getJobDistribution();

    PageResponse<PendingJobItem> getPendingJobs(int page, int pageSize);
}
