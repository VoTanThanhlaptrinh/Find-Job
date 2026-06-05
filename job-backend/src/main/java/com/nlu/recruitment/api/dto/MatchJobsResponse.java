package com.nlu.recruitment.api.dto;

import java.util.List;

public record MatchJobsResponse(String status,
                                Long cvId,
                                Integer candidateExperience,
                                Integer totalMatchesFound,
                                List<JobMatchItem> data) {
}
