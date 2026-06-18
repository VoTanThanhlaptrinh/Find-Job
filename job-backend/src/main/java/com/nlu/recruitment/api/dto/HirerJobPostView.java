package com.nlu.recruitment.api.dto;

public record HirerJobPostView(
        long id,
        String title,
        String description,
        String address,
        String salary,
        String time,
        int applies,
        int headcount,
        boolean isAnalyzed
) {
}
