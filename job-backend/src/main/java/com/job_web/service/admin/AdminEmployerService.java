package com.job_web.service.admin;

import com.job_web.dto.admin.employer.EmployerDetail;
import com.job_web.dto.admin.employer.EmployerListItem;
import com.job_web.dto.admin.employer.EmployerMetricsResponse;
import com.job_web.dto.admin.employer.EmployerStatusRequest;
import com.job_web.dto.common.PageResponse;

public interface AdminEmployerService {
    EmployerMetricsResponse getEmployerMetrics();

    PageResponse<EmployerListItem> getEmployers(int page, int pageSize, String search, String kycStatus, String status);

    EmployerDetail getEmployerDetail(long id);

    void updateEmployerStatus(long id, EmployerStatusRequest request);
}
