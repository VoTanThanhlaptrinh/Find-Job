package com.job_web.admin.application;

import com.job_web.admin.api.dto.job.AdminJobListItem;
import com.job_web.admin.api.dto.job.AdminJobRequest;
import com.job_web.admin.api.dto.job.BulkActionRequest;
import com.job_web.admin.api.dto.job.JobMetricsResponse;
import com.job_web.shared.domain.model.PageResponse;

public interface AdminJobService {
    JobMetricsResponse getJobMetrics();

    PageResponse<AdminJobListItem> getJobs(int page, int pageSize, String search, String category, String status);

    void createJob(AdminJobRequest request);

    void updateJobStatus(long id, String status);

    void bulkJobAction(BulkActionRequest request);
}
