package com.job_web.admin.api.dto.employer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployerDetail {
    private String id;
    private String name;
    private String industry;
    private LocalDate registrationDate;
    private int activeJobs;
    private String kycStatus;
    private String accountStatus;
    private String contactEmail;
    private String contactPhone;
}
