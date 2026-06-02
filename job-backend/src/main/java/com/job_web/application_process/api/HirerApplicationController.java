package com.job_web.application_process.api;

import com.job_web.application_process.api.dto.CandidateDTO;
import com.job_web.shared.domain.model.ApiResponse;
import com.job_web.application_process.application.JobApplicationService;
import com.job_web.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/hirer/applications", produces = "application/json")
@RequiredArgsConstructor
public class HirerApplicationController {
    private final JobApplicationService jobApplicationService;

    @GetMapping("/jobs/{jobId}/candidates/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<CandidateDTO>>> getAllCandidateAppliedJob(@PathVariable int pageIndex,
                                                                                     @PathVariable int pageSize,
                                                                                     @PathVariable long jobId) {
        Page<CandidateDTO> page = jobApplicationService.getAllCandidateAppliedJob(pageIndex, pageSize, jobId);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"),page, HttpStatus.OK.value()));
    }
}
