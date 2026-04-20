package com.job_web.dto.admin.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BulkActionRequest {
    private List<String> jobIds;
    private String action;
}
