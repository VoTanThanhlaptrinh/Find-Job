package com.job_web.dto.job;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class VectorizeJdRequest {
    private Long jobId;
    private Long userId;
    private Integer requiredYearsExperience;
    private LocalDate deadlineDate; // Hoặc String nếu bạn muốn format thủ công
    private JdDataContext data;
}
