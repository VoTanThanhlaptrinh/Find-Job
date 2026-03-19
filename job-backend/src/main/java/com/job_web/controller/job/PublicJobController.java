package com.job_web.controller.job;

import java.util.List;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobFilterDTO;
import com.job_web.models.Job;
import com.job_web.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/jobs", produces = "application/json")
@RequiredArgsConstructor
public class PublicJobController {
    private final JobService jobService;

    @GetMapping("/newest/{pageIndex}/{pageSize}")
    public ResponseEntity<ApiResponse<Page<Job>>> getListJobNewest(@PathVariable int pageIndex, @PathVariable int pageSize) {
        ApiResponse<Page<Job>> res = jobService.getListJobNewest(pageIndex, pageSize);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getAmount() {
        ApiResponse<Integer> res = jobService.getAmount();
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/address-count")
    public ResponseEntity<ApiResponse<List<AddressJobCount>>> getAddressCount() {
        ApiResponse<List<AddressJobCount>> res = jobService.getAddressJobCount();
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/filter")
    public ResponseEntity<ApiResponse<Page<Job>>> filterWithAddressTimeSalary(@RequestBody JobFilterDTO jobFilterDTO) {
        ApiResponse<Page<Job>> res = jobService.filterBetterSalaryAndHasAddressAndInTimes(
                jobFilterDTO.getPageIndex(),
                jobFilterDTO.getPageSize(),
                jobFilterDTO.getMin(),
                jobFilterDTO.getMax(),
                jobFilterDTO.getAddress(),
                jobFilterDTO.getTimes());
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Job>> getJobDetail(@PathVariable String id) {
        ApiResponse<Job> res = jobService.getJobDetailById(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/{id}/exists")
    public ResponseEntity<ApiResponse<Boolean>> checkApplyJob(@PathVariable String id) {
        ApiResponse<Boolean> res = jobService.checkExistJob(id);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
