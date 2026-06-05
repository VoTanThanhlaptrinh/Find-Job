package com.nlu.applicationProcess.api;

import com.nlu.applicationProcess.api.dto.req.ResumeDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeUrlDTO;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.applicationProcess.application.ResumeService;
import com.nlu.shared.utils.MessageUtils;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/hirer/resumes", produces = "application/json")
@RequiredArgsConstructor
public class HirerResumeController {
    private final ResumeService resumeService;

    @GetMapping("/users/{email}")
    public ResponseEntity<ApiResponse<List<ResumeDTO>>> listResumesByUser(@PathVariable @Email String email) {
        List<ResumeDTO> res = resumeService.getResumesByUser(email);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), res, HttpStatus.OK.value()));

    }

    @GetMapping("/{id}/view")
    public ResponseEntity<ApiResponse<ResumeUrlDTO>> getResumeViewUrl(@PathVariable("id") long id) {
        ResumeUrlDTO res = resumeService.getResumeViewUrlForHirer(id);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), res, HttpStatus.OK.value()));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<ResumeUrlDTO>> getResumeDownloadUrl(@PathVariable("id") long id) {
        ResumeUrlDTO res = resumeService.getResumeDownloadUrlForHirer(id);
        return ResponseEntity.ok().body(new ApiResponse<>(MessageUtils.getMessage("message.success"), res, HttpStatus.OK.value()));
    }
}
