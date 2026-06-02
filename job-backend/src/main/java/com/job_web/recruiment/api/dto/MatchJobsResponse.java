package com.job_web.recruiment.api.dto;

import java.util.List;

public record MatchJobsResponse(String status,
                                Long cvId,
                                Integer candidateExperience,
                                Integer totalMatchesFound,
                                List<JobMatchItem> data) {
}
