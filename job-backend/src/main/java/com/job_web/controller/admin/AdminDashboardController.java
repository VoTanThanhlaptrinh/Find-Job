package com.job_web.controller.admin;

import com.job_web.dto.admin.dashboard.DashboardSummaryResponse;
import com.job_web.dto.admin.dashboard.JobDistributionResponse;
import com.job_web.dto.admin.dashboard.PendingJobItem;
import com.job_web.dto.admin.dashboard.RevenueTrendResponse;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.common.PageResponse;
import com.job_web.service.admin.AdminService;
import com.job_web.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/admin", produces = "application/json")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminService adminService;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getDashboardSummary(), 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping("/dashboard/revenue-trend")
    public ResponseEntity<ApiResponse<RevenueTrendResponse>> getRevenueTrend(@RequestParam(defaultValue = "30d") String range) {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getRevenueTrend(range), 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping("/dashboard/job-distribution")
    public ResponseEntity<ApiResponse<JobDistributionResponse>> getJobDistribution() {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getJobDistribution(), 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping("/jobs/pending")
    public ResponseEntity<ApiResponse<PageResponse<PendingJobItem>>> getPendingJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getPendingJobs(page, pageSize), 
                HttpStatus.OK.value()
        ));
    }
}
