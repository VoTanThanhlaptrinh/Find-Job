package com.job_web.controller.application;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobApply;
import com.job_web.service.application.ApplyService;
import com.job_web.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserApplicationController {
    private final ApplyService applyService;
    private final JobService jobService;

    @PostMapping(path = "/api/apply/submit", produces = "application/json")
    public ResponseEntity<Map<String, String>> postSubmitResume(@RequestBody Map<String, Object> req) {
        Map<String, String> res = new HashMap<>();
        return ResponseEntity.ok(res);
    }

    @GetMapping(path = "/api/apply/pri/u/hasApplied/{jobId}", produces = "application/json")
    public ResponseEntity<ApiResponse<Boolean>> hasApplied(@PathVariable("jobId") long jobId, Principal principal) {
        ApiResponse<Boolean> res = applyService.hasApplied(principal, jobId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping(path = "/api/job/pri/u/listJobUserApplied/{pageIndex}/{pageSize}", produces = "application/json")
    public ResponseEntity<ApiResponse<Page<JobApply>>> listJobUserApplied(@PathVariable("pageIndex") int pageIndex,
                                                                          @PathVariable("pageSize") int pageSize,
                                                                          Principal principal) {
        ApiResponse<Page<JobApply>> res = jobService.listJobUserApplied(PageRequest.of(pageIndex, pageSize), principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
