package com.job_web.dto.job;

public record JobMatchItem(Long jobId,
                           Long employerId,
                           Integer requiredYears,
                           Double skillSimilarity,
                           Double experienceSimilarity,
                           Double overallMatchScore) {

}
