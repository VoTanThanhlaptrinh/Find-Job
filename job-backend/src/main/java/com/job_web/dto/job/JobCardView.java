package com.job_web.dto.job;

import com.job_web.constant.EmploymentType;

public record JobCardView(
        long id,
        String title,
        String address,
        String salary,
        EmploymentType time
) {
}
