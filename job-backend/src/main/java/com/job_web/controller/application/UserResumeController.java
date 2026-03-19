package com.job_web.controller.application;

import java.security.Principal;
import java.util.List;

import com.job_web.dto.application.ResumeDetailDTO;
import com.job_web.dto.application.ResumeDTO;
import com.job_web.dto.application.ResumeUploadDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.application.ResumeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/user/resumes", produces = "application/json")
@RequiredArgsConstructor
public class UserResumeController {
    private final ResumeService resumeService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeDTO>>> getListResumeOfUser(Principal principal) {
        ApiResponse<List<ResumeDTO>> res = resumeService.getListResumeOfUser(principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeDetailDTO>> getResumeDetail(@PathVariable("id") long id, Principal principal) {
        ApiResponse<ResumeDetailDTO> res = resumeService.getResumeDetail(id, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> uploadResume(@Valid @ModelAttribute ResumeUploadDTO resumeUploadDTO,
                                                            BindingResult bindingResult,
                                                            Principal principal) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = resumeService.createResume(resumeUploadDTO, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> updateResume(@PathVariable("id") long id,
                                                            @Valid @ModelAttribute ResumeUploadDTO resumeUploadDTO,
                                                            BindingResult bindingResult,
                                                            Principal principal) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = resumeService.updateResume(id, resumeUploadDTO, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteResume(@PathVariable("id") long id, Principal principal) {
        ApiResponse<String> res = resumeService.deleteResume(id, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}

