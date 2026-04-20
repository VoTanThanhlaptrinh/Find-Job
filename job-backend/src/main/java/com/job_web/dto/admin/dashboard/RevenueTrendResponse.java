package com.job_web.dto.admin.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RevenueTrendResponse {
    private String range;
    private List<String> labels;
    private List<Double> current;
    private List<Double> previous;
}
