package com.job_web.service.admin;

import com.job_web.dto.admin.job.AdminJobListItem;
import com.job_web.dto.admin.job.AdminJobRequest;
import com.job_web.dto.admin.job.BulkActionRequest;
import com.job_web.dto.admin.job.JobMetricsResponse;
import com.job_web.dto.common.PageResponse;

public interface AdminJobService {
    JobMetricsResponse getJobMetrics();

    PageResponse<AdminJobListItem> getJobs(int page, int pageSize, String search, String category, String status);

    void createJob(AdminJobRequest request);

    void updateJobStatus(long id, String status);

    void bulkJobAction(BulkActionRequest request);
}
