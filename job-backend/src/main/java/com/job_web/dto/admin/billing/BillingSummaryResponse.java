package com.job_web.dto.admin.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BillingSummaryResponse {
    private double monthlyRecurringRevenue;
    private double mrrGrowthPct;
    private long activeSubscriptions;
}
