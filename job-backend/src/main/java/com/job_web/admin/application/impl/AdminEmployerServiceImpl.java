package com.job_web.admin.application.impl;

import com.job_web.recruiment.domain.repository.RecruitmentRepository;
import com.job_web.admin.infrastructure.query.RecruitmentQuery;
import com.job_web.admin.api.dto.employer.EmployerDetail;
import com.job_web.admin.api.dto.employer.EmployerListItem;
import com.job_web.admin.api.dto.employer.EmployerMetricsResponse;
import com.job_web.admin.api.dto.employer.EmployerStatusRequest;
import com.job_web.shared.domain.model.PageResponse;
import com.job_web.shared.domain.exception.ResourceNotFoundException;
import com.job_web.recruiment.domain.model.Recruitment;
import com.job_web.admin.application.AdminEmployerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AdminEmployerServiceImpl implements AdminEmployerService {

    private final RecruitmentRepository recruitmentRepository;
    private final RecruitmentQuery recruitmentQuery;
    private final AdminPaginationMapper paginationMapper;

    @Override
    public EmployerMetricsResponse getEmployerMetrics() {
        long total = recruitmentRepository.count();
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
        Page<EmployerListItem> employers = recruitmentQuery.findEmployers(search, kycStatus, status, pageable);
        return PageResponse.<EmployerListItem>builder()
                .items(employers.getContent())
                .pagination(paginationMapper.map(page, pageSize, employers))
                .build();
    }

    @Override
    public EmployerDetail getEmployerDetail(long id) {
        Recruitment recruitment = recruitmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found"));

        return EmployerDetail.builder()
                .id(String.valueOf(recruitment.getId()))
                .name(recruitment.getCompanyName())
                .industry("Technology")
                .registrationDate(LocalDate.from(recruitment.getCreateDate()))
                .activeJobs(recruitment.getJobsPost() != null ? recruitment.getJobsPost().size() : 0)
                .kycStatus("verified")
                .accountStatus(recruitment.getStatus())
                .contactEmail(recruitment.getUser() != null ? recruitment.getUser().getEmail() : "")
                .contactPhone(recruitment.getUser() != null ? recruitment.getUser().getMobile() : "")
                .build();
    }

    @Override
    public void updateEmployerStatus(long id, EmployerStatusRequest request) {
        Recruitment recruitment = recruitmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employer not found"));
        if ("suspend".equalsIgnoreCase(request.getAction())) {
            recruitment.setStatus("SUSPENDED");
        } else if ("activate".equalsIgnoreCase(request.getAction())) {
            recruitment.setStatus("ACTIVE");
        }
        recruitmentRepository.save(recruitment);
    }
}
