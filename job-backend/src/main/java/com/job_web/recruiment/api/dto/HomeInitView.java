package com.job_web.recruiment.api.dto;

import java.util.List;

public record HomeInitView(
        List<JobCardView> jobSoon
) {
}
