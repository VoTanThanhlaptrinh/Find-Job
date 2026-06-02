package com.job_web.admin.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardSummaryResponse {
    private long totalEmployers;
    private long totalJobSeekers;
    private long pendingJobs;
    private double totalRevenue;
    private Growth growth;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Growth {
        private double employersPct;
        private double jobSeekersPct;
        private double revenuePct;
    }
}
