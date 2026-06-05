package com.nlu.applicationProcess.api.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MatchJobsResponse(
        @JsonProperty("status")
        String status,

        @JsonProperty("cvId")
        Long cvId,

        @JsonProperty("candidateExperience")
        Integer candidateExperience,

        @JsonProperty("totalMatchesFound")
        Integer totalMatchesFound,

        @JsonProperty("data")
        List<JobMatchItem> data
) {
}
