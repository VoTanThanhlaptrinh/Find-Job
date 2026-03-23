package com.job_web.dto.job;

import java.util.List;

public record HomeInitView(
        List<JobCardView> jobSoon
) {
}
