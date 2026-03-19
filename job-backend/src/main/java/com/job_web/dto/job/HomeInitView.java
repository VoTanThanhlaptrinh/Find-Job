package com.job_web.dto.job;

public record HomeInitView(
        PagedPayload<JobCardView> jobSalary,
        PagedPayload<JobCardView> jobSoon
) {
}
