package com.job_web.admin.application.impl;

import com.job_web.identity.domain.vo.EmailAddress;
import com.job_web.application_process.domain.repository.CandidateRepository;
import com.job_web.admin.infrastructure.query.CandidateQuery;
import com.job_web.admin.api.dto.seeker.JobSeekerListItem;
import com.job_web.admin.api.dto.seeker.JobSeekerMetricsResponse;
import com.job_web.admin.api.dto.seeker.JobSeekerRequest;
import com.job_web.admin.api.dto.seeker.RegionDistributionResponse;
import com.job_web.shared.domain.model.PageResponse;
import com.job_web.application_process.domain.model.Candidate;
import com.job_web.admin.application.AdminJobSeekerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminJobSeekerServiceImpl implements AdminJobSeekerService {

    private final CandidateRepository candidateRepository;
    private final CandidateQuery candidateQuery;
    private final AdminPaginationMapper paginationMapper;

    @Override
    public JobSeekerMetricsResponse getJobSeekerMetrics() {
        long total = candidateRepository.count();
        return JobSeekerMetricsResponse.builder()
                .totalSeekers(total)
                .totalSeekersGrowthPct(12.0)
                .activeLast7Days((long) (total * 0.3))
                .placedCandidates((long) (total * 0.2))
                .retentionPct(94.0)
                .build();
    }

    @Override
    public PageResponse<JobSeekerListItem> getJobSeekers(int page, int pageSize, String search, String resumeStatus) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<JobSeekerListItem> seekers = candidateQuery.findJobSeekers(search, resumeStatus, pageable);
        return PageResponse.<JobSeekerListItem>builder()
                .items(seekers.getContent())
                .pagination(paginationMapper.map(page, pageSize, seekers))
                .build();
    }

    @Override
    public void createJobSeeker(JobSeekerRequest request) {
        Candidate candidate = new Candidate();
        candidate.setFullName(request.getFullName());
        candidate.setEmail(new EmailAddress(request.getEmail()));
        candidate.setCreateDate(LocalDateTime.now());
        candidate.setStatus("ACTIVE");
        candidateRepository.save(candidate);
    }

    @Override
    public RegionDistributionResponse getRegionDistribution() {
        return RegionDistributionResponse.builder()
                .regions(List.of(
                        new RegionDistributionResponse.RegionCount("NA", 12000),
                        new RegionDistributionResponse.RegionCount("EU", 17000),
                        new RegionDistributionResponse.RegionCount("APAC", 9000),
                        new RegionDistributionResponse.RegionCount("LATAM", 5000),
                        new RegionDistributionResponse.RegionCount("MEA", 2200)))
                .build();
    }
}
