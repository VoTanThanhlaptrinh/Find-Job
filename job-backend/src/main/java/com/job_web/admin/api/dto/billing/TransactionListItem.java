package com.job_web.admin.api.dto.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TransactionListItem {
    private String id;
    private String employerId;
    private String employerName;
    private String packageName;
    private double amount;
    private String currency;
    private LocalDateTime date;
    private String status;
}
