package com.nlu.admin.application.impl;

import com.nlu.applicationProcess.domain.repository.CandidateRepository;
import com.nlu.recruitment.domain.repository.RecruitmentRepository;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.admin.api.dto.dashboard.DashboardSummaryResponse;
import com.nlu.admin.api.dto.dashboard.JobDistributionResponse;
import com.nlu.admin.api.dto.dashboard.PendingJobItem;
import com.nlu.admin.api.dto.dashboard.RevenueTrendResponse;
import com.nlu.shared.domain.model.PageResponse;
import com.nlu.recruitment.domain.model.Job;
import com.nlu.admin.application.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final JobRepository jobRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final CandidateRepository candidateRepository;
    private final AdminPaginationMapper paginationMapper;

    @Override
    public DashboardSummaryResponse getDashboardSummary() {
        return DashboardSummaryResponse.builder()
                .totalEmployers(recruitmentRepository.count())
                .totalJobSeekers(candidateRepository.count())
                .pendingJobs(jobRepository.countByStatus("PENDING"))
                .totalRevenue(142500.0)
                .growth(DashboardSummaryResponse.Growth.builder()
                        .employersPct(12.0)
                        .jobSeekersPct(8.0)
                        .revenuePct(15.0)
                        .build())
                .build();
    }

    @Override
    public RevenueTrendResponse getRevenueTrend(String range) {
        return RevenueTrendResponse.builder()
                .range(range)
                .labels(List.of("Week 1", "Week 2", "Week 3", "Week 4"))
                .current(List.of(7200.0, 9100.0, 12600.0, 15800.0))
                .previous(List.of(6100.0, 7900.0, 9500.0, 12100.0))
                .build();
    }

    @Override
    public JobDistributionResponse getJobDistribution() {
        long total = jobRepository.count();
        if (total == 0) {
            return JobDistributionResponse.builder().build();
        }

        return JobDistributionResponse.builder()
                .total(total)
                .activePct((double) jobRepository.countByStatus("ACTIVE") * 100 / total)
                .pendingPct((double) jobRepository.countByStatus("PENDING") * 100 / total)
                .expiredPct((double) jobRepository.countByStatus("EXPIRED") * 100 / total)
                .build();
    }

    @Override
    public PageResponse<PendingJobItem> getPendingJobs(int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<Job> jobs = jobRepository.findByStatus("PENDING", pageable);

        return PageResponse.<PendingJobItem>builder()
                .items(jobs.getContent().stream()
                        .map(job -> PendingJobItem.builder()
                                .id(String.valueOf(job.getId()))
                                .title(job.getTitle())
                                .subtitle("Required: " + job.getSkill())
                                .company(job.getRecruitment() != null ? job.getRecruitment().getCompanyName() : "Unknown")
                                .postDate(job.getCreateDate().toLocalDate())
                                .status("pending")
                                .build())
                        .collect(Collectors.toList()))
                .pagination(paginationMapper.map(page, pageSize, jobs))
                .build();
    }
}
