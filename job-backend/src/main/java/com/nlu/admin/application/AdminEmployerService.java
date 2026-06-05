package com.nlu.admin.application;

import com.nlu.admin.api.dto.employer.EmployerDetail;
import com.nlu.admin.api.dto.employer.EmployerListItem;
import com.nlu.admin.api.dto.employer.EmployerMetricsResponse;
import com.nlu.admin.api.dto.employer.EmployerStatusRequest;
import com.nlu.shared.domain.model.PageResponse;

public interface AdminEmployerService {
    EmployerMetricsResponse getEmployerMetrics();

    PageResponse<EmployerListItem> getEmployers(int page, int pageSize, String search, String kycStatus, String status);

    EmployerDetail getEmployerDetail(long id);

    void updateEmployerStatus(long id, EmployerStatusRequest request);
}
