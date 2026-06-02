package com.job_web.recruiment.api.dto;

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
