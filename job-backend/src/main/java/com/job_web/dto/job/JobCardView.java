package com.job_web.dto.job;

public record JobCardView(
        long id,
        String title,
        String address,
        String description,
        double salary,
        String time
) {
}
