package com.job_web.admin.api;

import com.job_web.admin.api.dto.employer.EmployerDetail;
import com.job_web.admin.api.dto.employer.EmployerListItem;
import com.job_web.admin.api.dto.employer.EmployerMetricsResponse;
import com.job_web.admin.api.dto.employer.EmployerStatusRequest;
import com.job_web.shared.domain.model.ApiResponse;
import com.job_web.shared.domain.model.PageResponse;
import com.job_web.admin.application.AdminService;
import com.job_web.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/admin/employers", produces = "application/json")
@RequiredArgsConstructor
public class AdminEmployersController {
    private final AdminService adminService;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<EmployerMetricsResponse>> getMetrics() {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getEmployerMetrics(), 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EmployerListItem>>> getEmployers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String kycStatus,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getEmployers(page, pageSize, search, kycStatus, status), 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EmployerDetail>> getEmployerDetail(@PathVariable long id) {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getEmployerDetail(id), 
                HttpStatus.OK.value()
        ));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateStatus(
            @PathVariable long id, 
            @RequestBody EmployerStatusRequest request) {
        adminService.updateEmployerStatus(id, request);
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                Map.of("id", String.valueOf(id), "updated", true), 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<ApiResponse<Map<String, String>>> export(
            @RequestParam(defaultValue = "csv") String format) {
        // Mock export URL
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                Map.of("downloadUrl", "https://cdn.example.com/exports/employers-2026-04-20.csv"), 
                HttpStatus.OK.value()
        ));
    }
}
