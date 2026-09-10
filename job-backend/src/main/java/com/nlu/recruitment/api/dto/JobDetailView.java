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
        Integer headcount,
        String companyName,
        String companyLogo,
        String companyDescription,
        String companyWebsite,
        Long companyId,
        String categoryName,
        Integer experienceYears,
        String locationCity
) {
}
