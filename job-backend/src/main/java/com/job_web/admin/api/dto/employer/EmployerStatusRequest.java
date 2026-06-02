package com.job_web.admin.api.dto.employer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployerStatusRequest {
    private String action;
    private String reason;
}
