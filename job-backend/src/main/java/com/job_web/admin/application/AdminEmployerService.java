package com.job_web.admin.application;

import com.job_web.admin.api.dto.employer.EmployerDetail;
import com.job_web.admin.api.dto.employer.EmployerListItem;
import com.job_web.admin.api.dto.employer.EmployerMetricsResponse;
import com.job_web.admin.api.dto.employer.EmployerStatusRequest;
import com.job_web.shared.domain.model.PageResponse;

public interface AdminEmployerService {
    EmployerMetricsResponse getEmployerMetrics();

    PageResponse<EmployerListItem> getEmployers(int page, int pageSize, String search, String kycStatus, String status);

    EmployerDetail getEmployerDetail(long id);

    void updateEmployerStatus(long id, EmployerStatusRequest request);
}
