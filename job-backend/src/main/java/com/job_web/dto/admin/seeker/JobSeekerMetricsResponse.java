package com.job_web.dto.admin.seeker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobSeekerMetricsResponse {
    private long totalSeekers;
    private double totalSeekersGrowthPct;
    private long activeLast7Days;
    private long placedCandidates;
    private double retentionPct;
}
