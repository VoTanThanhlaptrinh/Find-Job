package com.nlu.recruitment.api.dto;

import com.nlu.recruitment.domain.vo.EmploymentType;

public record JobCardView(
        long id,
        String title,
        String address,
        String salary,
        EmploymentType time
) {
}
