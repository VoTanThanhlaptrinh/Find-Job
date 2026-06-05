package com.nlu.admin.api.dto.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AdminJobListItem {
    private String id;
    private String title;
    private String company;
    private String location;
    private String category;
    private int applications;
    private int newApplicationsToday;
    private String status;
    private LocalDateTime expiryDate;
}
