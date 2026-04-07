package com.job_web.controller.application;

import java.io.IOException;

import com.job_web.dto.application.ApplyCvWithExistingRequest;
import com.job_web.dto.application.ApplyCvWithUploadRequest;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobCardView;
import com.job_web.models.CurrentUser;
import com.job_web.models.User;
import com.job_web.service.application.ApplyService;
import com.job_web.service.job.JobQueryService;
import com.job_web.utills.MessageUtils;
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
import org.springframework.web.bind.annotation.RequestParam;
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
                                                                   @CurrentUser User currentUser) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = applyService.applyWithExistingCv(request, currentUser);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping(value = "/applications/submit-upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> applyWithUploadCv(@Valid @ModelAttribute ApplyCvWithUploadRequest request,
                                                                 BindingResult bindingResult,
                                                                 @CurrentUser User currentUser) throws IOException {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = applyService.applyWithUploadCv(request, currentUser);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/applications/jobs/{jobId}/status")
    public ResponseEntity<ApiResponse<Boolean>> hasApplied(@PathVariable("jobId") long jobId, @CurrentUser User currentUser) {
        ApiResponse<Boolean> res = applyService.hasApplied(currentUser.getEmail(), jobId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/jobs/applied")
    public ResponseEntity<ApiResponse<Page<JobCardView>>> listJobUserApplied(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUser User currentUser) {
        Page<JobCardView> data = jobQueryService.listJobUserApplied(PageRequest.of(page, size), currentUser.getEmail());
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }
}
