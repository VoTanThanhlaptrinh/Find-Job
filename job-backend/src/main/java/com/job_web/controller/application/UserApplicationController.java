package com.job_web.controller.application;

import java.io.IOException;
import java.security.Principal;

import com.job_web.dto.application.ApplyCvWithExistingRequest;
import com.job_web.dto.application.ApplyCvWithUploadRequest;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobApply;
import com.job_web.service.application.ApplyService;
import com.job_web.service.job.JobQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/user", produces = "application/json")
@RequiredArgsConstructor
public class UserApplicationController {
    private final ApplyService applyService;
    private final JobQueryService jobQueryService;

    @PostMapping("/applications/submit-existing")
    public ResponseEntity<ApiResponse<String>> applyWithExistingCv(@Valid @RequestBody ApplyCvWithExistingRequest request,
                                                                   BindingResult bindingResult,
                                                                   Principal principal) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = applyService.applyWithExistingCv(request, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping(value = "/applications/submit-upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> applyWithUploadCv(@Valid @ModelAttribute ApplyCvWithUploadRequest request,
                                                                 BindingResult bindingResult,
                                                                 Principal principal) throws IOException {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = applyService.applyWithUploadCv(request, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/applications/jobs/{jobId}/status")
    public ResponseEntity<ApiResponse<Boolean>> hasApplied(@PathVariable("jobId") long jobId, Principal principal) {
        ApiResponse<Boolean> res = applyService.hasApplied(principal, jobId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/jobs/applied/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<JobApply>>> listJobUserApplied(@PathVariable("pageIndex") int pageIndex,
                                                                          @PathVariable("pageSize") int pageSize,
                                                                          Principal principal) {
        ApiResponse<Page<JobApply>> res = jobQueryService.listJobUserApplied(PageRequest.of(pageIndex, pageSize), principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
