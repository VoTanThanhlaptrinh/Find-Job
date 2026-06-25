package com.nlu.admin.api;

import com.nlu.admin.api.dto.seeker.JobSeekerListItem;
import com.nlu.admin.api.dto.seeker.JobSeekerMetricsResponse;
import com.nlu.admin.api.dto.seeker.JobSeekerRequest;
import com.nlu.admin.api.dto.seeker.RegionDistributionResponse;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.shared.domain.model.PageResponse;
import com.nlu.admin.application.AdminService;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping(path = "/api/admin/job-seekers", produces = "application/json")
@RequiredArgsConstructor
public class AdminJobSeekersController {
    private final AdminService adminService;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<JobSeekerMetricsResponse>> getMetrics() {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getJobSeekerMetrics(), 
                HttpStatus.OK.value()
        ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<JobSeekerListItem>>> getJobSeekers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String resumeStatus) {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getJobSeekers(page, pageSize, search, resumeStatus), 
                HttpStatus.OK.value()
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> createJobSeeker(@RequestBody JobSeekerRequest request) {
        adminService.createJobSeeker(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                Map.of("id", "cand_new", "created", true), 
                HttpStatus.CREATED.value()
        ));
    }

    @GetMapping("/region-distribution")
    public ResponseEntity<ApiResponse<RegionDistributionResponse>> getRegionDistribution() {
        return ResponseEntity.ok(new ApiResponse<>(
                MessageUtils.getMessage("message.success"), 
                adminService.getRegionDistribution(), 
                HttpStatus.OK.value()
        ));
    }
}
