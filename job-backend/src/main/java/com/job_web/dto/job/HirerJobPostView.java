package com.job_web.dto.job;

public record HirerJobPostView(
        long id,
        String title,
        String description,
        String address,
        String salary,
        String time,
        int applies,
        int headcount
) {
}
