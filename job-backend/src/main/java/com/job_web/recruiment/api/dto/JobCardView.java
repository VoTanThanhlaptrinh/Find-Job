package com.job_web.recruiment.api.dto;

import com.job_web.recruiment.domain.vo.EmploymentType;

public record JobCardView(
        long id,
        String title,
        String address,
        String salary,
        EmploymentType time
) {
}
