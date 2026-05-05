package com.job_web.service.admin.impl;

import com.job_web.data.HirerRepository;
import com.job_web.data.queryDSL.HirerQuery;
import com.job_web.dto.admin.employer.EmployerDetail;
import com.job_web.dto.admin.employer.EmployerListItem;
import com.job_web.dto.admin.employer.EmployerMetricsResponse;
import com.job_web.dto.admin.employer.EmployerStatusRequest;
import com.job_web.dto.common.PageResponse;
import com.job_web.exception.ResourceNotFoundException;
import com.job_web.models.Hirer;
import com.job_web.service.admin.AdminEmployerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class AdminEmployerServiceImpl implements AdminEmployerService {

    private final HirerRepository hirerRepository;
    private final HirerQuery hirerQuery;
    private final AdminPaginationMapper paginationMapper;

    @Override
    public EmployerMetricsResponse getEmployerMetrics() {
        long total = hirerRepository.count();
        return EmployerMetricsResponse.builder()
                .totalEmployers(total)
                .totalEmployersGrowthPct(12.0)
                .kycVerified((long) (total * 0.8))
                .kycVerifiedPct(80.0)
                .pendingKyc((long) (total * 0.1))
                .suspended((long) (total * 0.1))
                .build();
    }

    @Override
    public PageResponse<EmployerListItem> getEmployers(int page, int pageSize, String search, String kycStatus, String status) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<EmployerListItem> employers = hirerQuery.findEmployers(search, kycStatus, status, pageable);
        return PageResponse.<EmployerListItem>builder()
                .items(employers.getContent())
                .pagination(paginationMapper.map(page, pageSize, employers))
                .build();
    }

    @Override
    public EmployerDetail getEmployerDetail(long id) {
        Hirer hirer = hirerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found"));

        return EmployerDetail.builder()
                .id(String.valueOf(hirer.getId()))
                .name(hirer.getCompanyName())
                .industry("Technology")
                .registrationDate(LocalDate.from(hirer.getCreateDate()))
                .activeJobs(hirer.getJobsPost() != null ? hirer.getJobsPost().size() : 0)
                .kycStatus("verified")
                .accountStatus(hirer.getStatus())
                .contactEmail(hirer.getUser() != null ? hirer.getUser().getEmail() : "")
                .contactPhone(hirer.getUser() != null ? hirer.getUser().getMobile() : "")
                .build();
    }

    @Override
    public void updateEmployerStatus(long id, EmployerStatusRequest request) {
        Hirer hirer = hirerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found"));
        if ("suspend".equalsIgnoreCase(request.getAction())) {
            hirer.setStatus("SUSPENDED");
        } else if ("activate".equalsIgnoreCase(request.getAction())) {
            hirer.setStatus("ACTIVE");
        }
        hirerRepository.save(hirer);
    }
}
