package com.job_web.dto.ai;

public record JDEmbeddingRequest(
        Long jobId,
        Long companyId,
        Integer requiredYearsExperience,
        JobDescriptionModel data
) {}
