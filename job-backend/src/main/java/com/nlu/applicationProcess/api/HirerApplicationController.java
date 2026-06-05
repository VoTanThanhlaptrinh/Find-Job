package com.nlu.applicationProcess.api;

import com.nlu.applicationProcess.api.dto.req.CandidateDTO;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.applicationProcess.application.JobApplicationService;
import com.nlu.shared.utils.MessageUtils;
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
    public ResponseEntity<ApiResponse<Page<CandidateDTO>>> getCandidatesAppliedToJob(@PathVariable int pageIndex,
                                                                                     @PathVariable int pageSize,
                                                                                     @PathVariable long jobId) {
        Page<CandidateDTO> page = jobApplicationService.getCandidatesAppliedToJob(pageIndex, pageSize, jobId);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"),page, HttpStatus.OK.value()));
    }
}
