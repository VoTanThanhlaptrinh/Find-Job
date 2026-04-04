package com.job_web.controller.application;

import com.job_web.dto.application.ResumeDTO;
import com.job_web.dto.application.ResumeUrlDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.models.Resume;
import com.job_web.service.application.ResumeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    public ResponseEntity<ApiResponse<List<ResumeDTO>>> listResumesByUser(@PathVariable("email") String email) {
        ApiResponse<List<ResumeDTO>> res = resumeService.getResumesByUser(email);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    /**
     * Lấy Pre-signed URL để xem resume trực tiếp trên trình duyệt (inline) - dành cho Hirer.
     */
    @GetMapping("/{id}/view")
    public ResponseEntity<ApiResponse<ResumeUrlDTO>> getResumeViewUrl(@PathVariable("id") long id) {
        ApiResponse<ResumeUrlDTO> res = resumeService.getResumeViewUrlForHirer(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    /**
     * Lấy Pre-signed URL để tải resume về (attachment) - dành cho Hirer.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<ApiResponse<ResumeUrlDTO>> getResumeDownloadUrl(@PathVariable("id") long id) {
        ApiResponse<ResumeUrlDTO> res = resumeService.getResumeDownloadUrlForHirer(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
