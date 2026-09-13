package com.nlu.applicationProcess.api;

import com.nlu.applicationProcess.api.dto.req.ResumeUploadInitiateRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.api.dto.res.ResumeUploadInitiateResponse;
import com.nlu.applicationProcess.application.ResumeUploadService;
import com.nlu.identity.domain.model.CurrentUser;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.shared.utils.MessageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping(path = "/api/user/resume-uploads", produces = "application/json")
@RequiredArgsConstructor
public class ResumeUploadController {

    private final ResumeUploadService resumeUploadService;

    @PostMapping("/initiate")
    public ResponseEntity<ApiResponse<ResumeUploadInitiateResponse>> initiateUpload(
            @Valid @RequestBody ResumeUploadInitiateRequest request,
            @CurrentUser User currentUser) {

        ResumeUploadInitiateResponse response = resumeUploadService.initiateUpload(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(MessageUtils.getMessage("message.success"), response, HttpStatus.CREATED.value()));
    }

    @PostMapping("/{uploadId}/complete")
    public ResponseEntity<ApiResponse<ResumeView>> completeUpload(
            @PathVariable UUID uploadId,
            @CurrentUser User currentUser) {

        ResumeView resumeView = resumeUploadService.completeUpload(uploadId, currentUser);
        return ResponseEntity.ok()
                .body(new ApiResponse<>(MessageUtils.getMessage("message.success"), resumeView, HttpStatus.OK.value()));
    }
}
