package com.nlu.admin.application;

import com.nlu.admin.api.dto.job.AdminJobListItem;
import com.nlu.admin.api.dto.job.AdminJobRequest;
import com.nlu.admin.api.dto.job.BulkActionRequest;
import com.nlu.admin.api.dto.job.JobMetricsResponse;
import com.nlu.shared.domain.model.PageResponse;

public interface AdminJobService {
    JobMetricsResponse getJobMetrics();

    PageResponse<AdminJobListItem> getJobs(int page, int pageSize, String search, String category, String status);

    void createJob(AdminJobRequest request);

    void updateJobStatus(long id, String status);

    void bulkJobAction(BulkActionRequest request);
}
