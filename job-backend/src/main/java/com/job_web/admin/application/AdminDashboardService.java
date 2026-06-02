package com.job_web.admin.application;

import com.job_web.admin.api.dto.dashboard.DashboardSummaryResponse;
import com.job_web.admin.api.dto.dashboard.JobDistributionResponse;
import com.job_web.admin.api.dto.dashboard.PendingJobItem;
import com.job_web.admin.api.dto.dashboard.RevenueTrendResponse;
import com.job_web.shared.domain.model.PageResponse;

public interface AdminDashboardService {
    DashboardSummaryResponse getDashboardSummary();

    RevenueTrendResponse getRevenueTrend(String range);

    JobDistributionResponse getJobDistribution();

    PageResponse<PendingJobItem> getPendingJobs(int page, int pageSize);
}
