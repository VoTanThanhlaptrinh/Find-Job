package com.job_web.admin.api.dto.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminJobRequest {
    private String title;
    private String companyId;
    private String category;
    private String description;
    private String location;
    private LocalDateTime expiryDate;
}
