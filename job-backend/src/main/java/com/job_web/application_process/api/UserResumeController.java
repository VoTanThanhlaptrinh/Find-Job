package com.job_web.application_process.api;

import java.util.List;

import com.job_web.application_process.api.dto.ResumeDetailDTO;
import com.job_web.application_process.api.dto.ResumeUploadDTO;
import com.job_web.application_process.api.dto.ResumeUrlDTO;
import com.job_web.application_process.api.dto.ResumeView;
import com.job_web.shared.domain.model.ApiResponse;
import com.job_web.identity.domain.model.CurrentUser;
import com.job_web.identity.domain.model.User;
import com.job_web.application_process.application.ResumeService;
import com.job_web.shared.utils.MessageUtils;
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
    public ResponseEntity<ApiResponse<List<ResumeView>>> getListResumeOfUser(@CurrentUser User currentUser) {
        if(currentUser == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(MessageUtils.getMessage("message.unauthorized"), null, HttpStatus.BAD_REQUEST.value()));
        }
        List<ResumeView> resumes = resumeService.getListResumeOfUser(currentUser);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"),resumes, HttpStatus.OK.value()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeDetailDTO>> getResumeDetail(@PathVariable long id, @CurrentUser User currentUser) {
        ResumeDetailDTO res = resumeService.getResumeDetail(id, currentUser);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), res, HttpStatus.OK.value()));
    }

    @GetMapping("/{id}/view")
    public ResponseEntity<ApiResponse<ResumeUrlDTO>> getResumeViewUrl(@PathVariable long id, @CurrentUser User currentUser) {

        ResumeUrlDTO res = resumeService.getResumeViewUrl(id, currentUser);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), res, HttpStatus.OK.value()));
    }
    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<ResumeUrlDTO>> getResumeDownloadUrl(@PathVariable long id, @CurrentUser User currentUser) {
        ResumeUrlDTO res = resumeService.getResumeDownloadUrl(id, currentUser);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), res, HttpStatus.OK.value()));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<ResumeView>> uploadResume(@Valid @ModelAttribute ResumeUploadDTO resumeUploadDTO,
                                                            BindingResult bindingResult,
                                                            @CurrentUser User currentUser) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        var res = resumeService.createResume(resumeUploadDTO, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponse<>(MessageUtils.getMessage("message.success"), res, HttpStatus.CREATED.value()));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<String>> updateResume(@PathVariable("id") long id,
                                                            @Valid @ModelAttribute ResumeUploadDTO resumeUploadDTO,
                                                            BindingResult bindingResult,
                                                            @CurrentUser User currentUser) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        resumeService.updateResume(id, resumeUploadDTO, currentUser);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), null, HttpStatus.OK.value()));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteResume(@PathVariable("id") long id, @CurrentUser User currentUser) {
        resumeService.deleteResume(id, currentUser);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), null, HttpStatus.OK.value()));
    }
}
