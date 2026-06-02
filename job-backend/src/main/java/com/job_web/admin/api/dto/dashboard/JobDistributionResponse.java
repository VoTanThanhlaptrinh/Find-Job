package com.job_web.admin.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class JobDistributionResponse {
    private long total;
    private double activePct;
    private double pendingPct;
    private double expiredPct;
}
