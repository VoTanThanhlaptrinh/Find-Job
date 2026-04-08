package com.job_web.controller.application;

import com.job_web.dto.application.CandidateDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.application.ApplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/hirer/applications", produces = "application/json")
@RequiredArgsConstructor
public class HirerApplicationController {
    private final ApplyService applyService;

    @GetMapping("/jobs/{jobId}/candidates/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<CandidateDTO>>> getAllCandidateAppliedJob(@PathVariable int pageIndex,
                                                                                     @PathVariable int pageSize,
                                                                                     @PathVariable long jobId) {
        ApiResponse<Page<CandidateDTO>> res = applyService.getAllCandidateAppliedJob(pageIndex, pageSize, jobId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
