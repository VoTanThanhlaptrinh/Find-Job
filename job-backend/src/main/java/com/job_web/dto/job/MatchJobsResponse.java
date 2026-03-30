package com.job_web.dto.job;

import java.util.List;

public record MatchJobsResponse(String status,
                                Long cvId,
                                Integer candidateExperience,
                                Integer totalMatchesFound,
                                List<JobMatchItem> data) {
}
