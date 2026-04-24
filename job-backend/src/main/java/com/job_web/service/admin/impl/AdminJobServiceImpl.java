package com.job_web.service.admin.impl;

import com.job_web.data.ApplyRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.queryDSL.AdminJobQuery;
import com.job_web.dto.admin.job.AdminJobListItem;
import com.job_web.dto.admin.job.AdminJobRequest;
import com.job_web.dto.admin.job.BulkActionRequest;
import com.job_web.dto.admin.job.JobMetricsResponse;
import com.job_web.dto.common.PageResponse;
import com.job_web.exception.ResourceNotFoundException;
import com.job_web.models.Job;
import com.job_web.service.admin.AdminJobService;
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
    private final ApplyRepository applyRepository;
    private final AdminJobQuery adminJobQuery;
    private final AdminPaginationMapper paginationMapper;

    @Override
    public JobMetricsResponse getJobMetrics() {
        return JobMetricsResponse.builder()
                .livePostings(jobRepository.countByStatus("ACTIVE"))
                .livePostingsGrowthPct(12.0)
                .pendingReview(jobRepository.countByStatus("PENDING"))
                .totalApplicants(applyRepository.count())
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
        job.setStatus("PENDING");
        job.setCreateDate(LocalDateTime.now());
        jobRepository.save(job);
    }

    @Override
    public void updateJobStatus(long id, String status) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        job.setStatus(status.toUpperCase());
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
