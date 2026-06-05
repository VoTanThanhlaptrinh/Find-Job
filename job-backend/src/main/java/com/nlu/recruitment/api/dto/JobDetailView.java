package com.nlu.recruitment.api.dto;

public record JobDetailView(
        long id,
        String title,
        String address,
        String description,
        String salary,
        String time,
        String requireDetails,
        String skill,
        String expiredDate,
        Integer headcount
) {
}
