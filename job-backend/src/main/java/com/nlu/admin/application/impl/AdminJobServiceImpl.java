package com.nlu.admin.application.impl;

import com.nlu.applicationProcess.domain.repository.JobApplicationRepository;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.admin.infrastructure.query.AdminJobQuery;
import com.nlu.admin.api.dto.job.AdminJobListItem;
import com.nlu.admin.api.dto.job.AdminJobRequest;
import com.nlu.admin.api.dto.job.BulkActionRequest;
import com.nlu.admin.api.dto.job.JobMetricsResponse;
import com.nlu.shared.domain.model.EntityStatus;
import com.nlu.shared.domain.model.PageResponse;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.recruitment.domain.model.Job;
import com.nlu.admin.application.AdminJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AdminJobServiceImpl implements AdminJobService {

    private final JobRepository jobRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final AdminJobQuery adminJobQuery;
    private final AdminPaginationMapper paginationMapper;

    @Override
    public JobMetricsResponse getJobMetrics() {
        return JobMetricsResponse.builder()
                .livePostings(jobRepository.countByRecordStatus(EntityStatus.ACTIVE))
                .livePostingsGrowthPct(12.0)
                .pendingReview(jobRepository.countByRecordStatus(EntityStatus.PENDING))
                .totalApplicants(jobApplicationRepository.count())
                .avgTimeToHireDays(18)
                .build();
    }

    @Override
    public PageResponse<AdminJobListItem> getJobs(int page, int pageSize, String search, String category, String status) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);
        Page<AdminJobListItem> jobs = adminJobQuery.findJobs(search, category, status, pageable);
        return PageResponse.<AdminJobListItem>builder()
                .items(jobs.getContent())
                .pagination(paginationMapper.map(page, pageSize, jobs))
                .build();
    }

    @Override
    public void createJob(AdminJobRequest request) {
        Job job = new Job();
        job.setTitle(request.getTitle());
        job.setDescription(request.getDescription());
        job.setExpiredDate(request.getExpiryDate());
        job.setRecordStatus(com.nlu.shared.domain.model.EntityStatus.PENDING);
        jobRepository.save(job);
    }

    @Override
    public void updateJobStatus(long id, String status) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.setRecordStatus(com.nlu.shared.domain.model.EntityStatus.valueOf(status.toUpperCase()));
        jobRepository.save(job);
    }

    @Override
    public void bulkJobAction(BulkActionRequest request) {
        for (String jobId : request.getJobIds()) {
            try {
                updateJobStatus(Long.parseLong(jobId), request.getAction());
            } catch (Exception ignored) {
            }
        }
    }
}
