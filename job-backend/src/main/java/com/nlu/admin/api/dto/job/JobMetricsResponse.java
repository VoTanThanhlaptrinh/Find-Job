package com.nlu.admin.api.dto.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobMetricsResponse {
    private long livePostings;
    private double livePostingsGrowthPct;
    private long pendingReview;
    private long totalApplicants;
    private int avgTimeToHireDays;
}
