package com.job_web.dto.job;

public record JobDetailView(
        long id,
        String title,
        String address,
        String description,
        double salary,
        String time,
        String requireDetails,
        String skill,
        String expiredDate
) {
}
