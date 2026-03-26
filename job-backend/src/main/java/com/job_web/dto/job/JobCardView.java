package com.job_web.dto.job;

public record JobCardView(
        long id,
        String title,
        String address,
        String salary,
        String time
) {
}
