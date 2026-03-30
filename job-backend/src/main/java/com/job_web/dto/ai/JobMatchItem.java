package com.job_web.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;

public record JobMatchItem(
        @JsonProperty("jobId")
        Long jobId,

        @JsonProperty("employerId")
        Long employerId,

        @JsonProperty("requiredYears")
        Integer requiredYears,

        @JsonProperty("skillSimilarity")
        Double skillSimilarity,

        @JsonProperty("experienceSimilarity")
        Double experienceSimilarity,

        @JsonProperty("overallMatchScore")
        Double overallMatchScore
) {
}
