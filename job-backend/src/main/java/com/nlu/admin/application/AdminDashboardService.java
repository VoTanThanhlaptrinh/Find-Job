package com.nlu.admin.application;

import com.nlu.admin.api.dto.dashboard.DashboardSummaryResponse;
import com.nlu.admin.api.dto.dashboard.JobDistributionResponse;
import com.nlu.admin.api.dto.dashboard.PendingJobItem;
import com.nlu.admin.api.dto.dashboard.RevenueTrendResponse;
import com.nlu.shared.domain.model.PageResponse;

public interface AdminDashboardService {
    DashboardSummaryResponse getDashboardSummary();

    RevenueTrendResponse getRevenueTrend(String range);

    JobDistributionResponse getJobDistribution();

    PageResponse<PendingJobItem> getPendingJobs(int page, int pageSize);
}
