package com.job_web.controller.job;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.JobResponse;
import com.job_web.models.CurrentUser;
import com.job_web.models.User;
import com.job_web.service.job.JobQueryService;
import com.job_web.service.job.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/hirer/jobs", produces = "application/json")
@RequiredArgsConstructor
public class HirerJobController {
    private final JobService jobService;
    private final JobQueryService jobQueryService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> post(@Valid @ModelAttribute JobDTO job,
                                                    @CurrentUser User currentUser) {
        jobService.createJob(job, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("Tạo việc làm thành công", null, HttpStatus.CREATED.value()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> update(@PathVariable("id") Long id,
                                                      @Valid @ModelAttribute JobDTO job,
                                                      @CurrentUser User currentUser) {
        jobService.updateJob(id, job, currentUser);
        return ResponseEntity.ok(new ApiResponse<>("Cập nhật việc làm thành công", null, HttpStatus.OK.value()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> delete(@PathVariable("id") Long id, @CurrentUser User currentUser) {
        jobService.deleteJob(id, currentUser);
        return ResponseEntity.ok(new ApiResponse<>("Xóa việc làm thành công", null, HttpStatus.OK.value()));
    }

    @GetMapping("/posted")
    public ResponseEntity<ApiResponse<Page<JobResponse>>> getHirerJobPost(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @CurrentUser User currentUser) {
        Page<JobResponse> data = jobQueryService.getHirerJobPost(page, size, currentUser.getEmail());
        return ResponseEntity.ok(new ApiResponse<>("success", data, HttpStatus.OK.value()));
    }

    @GetMapping("/posted/count")
    public ResponseEntity<ApiResponse<Long>> countHirerJobPost(@CurrentUser User currentUser) {
        Long data = jobQueryService.countHirerJobPost(currentUser.getEmail());
        return ResponseEntity.ok(new ApiResponse<>("success", data, HttpStatus.OK.value()));
    }
}
