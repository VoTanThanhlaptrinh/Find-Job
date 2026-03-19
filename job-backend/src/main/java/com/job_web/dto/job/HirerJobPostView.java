package com.job_web.dto.job;

public record HirerJobPostView(
        long id,
        String title,
        String description,
        String address,
        double salary,
        String time,
        int applies
) {
}
