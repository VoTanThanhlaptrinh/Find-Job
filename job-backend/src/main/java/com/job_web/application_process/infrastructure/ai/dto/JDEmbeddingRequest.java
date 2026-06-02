package com.job_web.application_process.infrastructure.ai.dto;

public record JDEmbeddingRequest(
        Long jobId,
        Long companyId,
        Integer requiredYearsExperience,
        JobDescriptionModel data
) {}
