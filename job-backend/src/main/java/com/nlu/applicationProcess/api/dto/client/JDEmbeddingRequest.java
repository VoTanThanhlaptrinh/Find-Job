package com.nlu.applicationProcess.api.dto.client;

public record JDEmbeddingRequest(
        Long jobId,
        Long companyId,
        Integer requiredYearsExperience,
        JobDescriptionModel data
) {}
