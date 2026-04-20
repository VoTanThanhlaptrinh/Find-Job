package com.job_web.dto.admin.employer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployerMetricsResponse {
    private long totalEmployers;
    private double totalEmployersGrowthPct;
    private long kycVerified;
    private double kycVerifiedPct;
    private long pendingKyc;
    private long suspended;
}
