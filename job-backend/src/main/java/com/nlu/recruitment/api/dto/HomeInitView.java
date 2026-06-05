package com.nlu.recruitment.api.dto;

import java.util.List;

public record HomeInitView(
        List<JobCardView> jobSoon
) {
}
