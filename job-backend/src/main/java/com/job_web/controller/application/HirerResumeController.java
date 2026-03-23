package com.job_web.controller.application;

import com.job_web.dto.application.ResumeDTO;
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

//    @GetMapping("/{id}/download")
//    public ResponseEntity<Resource> downloadResume(@PathVariable("id") long id) {
//        Resume resume = resumeService.findById(id);
//        if (resume == null) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
//        }
//        ByteArrayResource byteArrayResource = new ByteArrayResource(resume.getData());
//        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(byteArrayResource);
//    }
}
