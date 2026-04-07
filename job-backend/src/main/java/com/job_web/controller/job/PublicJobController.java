package com.job_web.controller.job;

import java.util.List;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobDetailView;
import com.job_web.dto.job.JobFilterDTO;
import com.job_web.service.ai.ApiService;
import com.job_web.service.job.JobQueryService;
import com.job_web.service.job.JobService;
import com.job_web.utills.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/jobs", produces = "application/json")
@RequiredArgsConstructor
public class PublicJobController {
    private final JobService jobService;
    private final JobQueryService jobQueryService;
    private final ApiService apiService;

    @GetMapping("/newest")
    public ResponseEntity<ApiResponse<Page<JobCardView>>> getListJobNewest(
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        Page<JobCardView> data = jobQueryService.getListJobNewest(page, size);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getAmount() {
        Integer data = jobQueryService.getAmount();
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @GetMapping("/address-count")
    public ResponseEntity<ApiResponse<List<AddressJobCount>>> getAddressCount() {
        List<AddressJobCount> data = jobQueryService.getAddressJobCount();
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @PostMapping("/filter")
    public ResponseEntity<ApiResponse<Page<JobCardView>>> filterWithAddressTimeSalary(@RequestBody JobFilterDTO jobFilterDTO) {
        Page<JobCardView> data = jobQueryService.filterBetterSalaryAndHasAddressAndInTimes(
                jobFilterDTO.getPageIndex(),
                jobFilterDTO.getPageSize(),
                jobFilterDTO.getMin(),
                jobFilterDTO.getMax(),
                jobFilterDTO.getAddress(),
                jobFilterDTO.getTimes(),
                jobFilterDTO.getTitle());
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobDetailView>> getJobDetail(@PathVariable Long id) {
        JobDetailView data = jobService.getJobDetailById(id);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkApplyJob(@PathVariable Long id) {
        Boolean data = jobService.checkExistJob(id);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @GetMapping("/match/{cvId}")
    public ResponseEntity<ApiResponse<List<JobCardView>>> getMatchedJobs(@PathVariable Long cvId) {
        List<JobCardView> jobs = apiService.matchJobs(cvId);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), jobs, HttpStatus.OK.value()));
    }
}
