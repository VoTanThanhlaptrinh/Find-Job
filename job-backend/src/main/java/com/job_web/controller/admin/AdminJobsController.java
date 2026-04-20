package com.job_web.controller.admin;

import com.job_web.dto.admin.job.AdminJobListItem;
import com.job_web.dto.admin.job.AdminJobRequest;
import com.job_web.dto.admin.job.BulkActionRequest;
import com.job_web.dto.admin.job.JobMetricsResponse;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.common.PageResponse;
import com.job_web.service.admin.AdminService;
import com.job_web.utills.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/admin/jobs", produces = "application/json")
@RequiredArgsConstructor
public class AdminJobsController {
    private final AdminService adminService;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<JobMetricsResponse>> getMetrics() {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getJobMetrics(), 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminJobListItem>>> getJobs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getJobs(page, pageSize, search, category, status), 
                HttpStatus.OK.value()
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createJob(@RequestBody AdminJobRequest request) {
        adminService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                Map.of("id", "job_new", "created", true), 
                HttpStatus.CREATED.value()
        ));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable long id, 
            @RequestBody Map<String, String> body) {
        adminService.updateJobStatus(id, body.get("status"));
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                Map.of("id", String.valueOf(id), "status", body.get("status")), 
                HttpStatus.OK.value()
        ));
    }

    @PostMapping("/bulk-action")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkAction(@RequestBody BulkActionRequest request) {
        adminService.bulkJobAction(request);
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                Map.of("processed", request.getJobIds().size(), "failed", 0), 
                HttpStatus.OK.value()
        ));
    }
}
