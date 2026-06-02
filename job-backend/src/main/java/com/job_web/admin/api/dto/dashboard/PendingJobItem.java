package com.job_web.admin.api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PendingJobItem {
    private String id;
    private String title;
    private String subtitle;
    private String company;
    private LocalDate postDate;
    private String status;
}
