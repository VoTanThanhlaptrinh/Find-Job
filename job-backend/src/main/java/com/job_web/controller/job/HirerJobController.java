package com.job_web.controller.job;

import java.security.Principal;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.JobResponse;
import com.job_web.service.job.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@RequestMapping(path = "/api/hirer/jobs", produces = "application/json")
@RequiredArgsConstructor
public class HirerJobController {
    private final JobService jobService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> post(@Valid @ModelAttribute JobDTO job,
                                                    BindingResult bindingResult,
                                                    Principal principal) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = jobService.createJob(job, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> update(@PathVariable("id") long id,
                                                      @Valid @ModelAttribute JobDTO job,
                                                      BindingResult bindingResult,
                                                      Principal principal) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, HttpStatus.BAD_REQUEST.value()));
        }
        ApiResponse<String> res = jobService.updateJob(id, job, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable("id") long id, Principal principal) {
        ApiResponse<String> res = jobService.deleteJob(id, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/posted/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getHirerJobPost(@PathVariable int pageIndex,
                                                                          @PathVariable int pageSize,
                                                                          Principal principal) {
        ApiResponse<Page<JobResponse>> res = jobService.getHirerJobPost(pageIndex, pageSize, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/posted/count")
    public ResponseEntity<ApiResponse<Long>> countHirerJobPost(Principal principal) {
        ApiResponse<Long> res = jobService.countHirerJobPost(principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
