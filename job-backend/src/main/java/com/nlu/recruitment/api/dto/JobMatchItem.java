package com.nlu.recruitment.api.dto;

public record JobMatchItem(Long jobId,
                           Long employerId,
                           Integer requiredYears,
                           Double skillSimilarity,
                           Double experienceSimilarity,
                           Double overallMatchScore) {

}
