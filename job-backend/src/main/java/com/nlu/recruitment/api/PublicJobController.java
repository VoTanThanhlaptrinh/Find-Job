package com.nlu.recruitment.api;

import java.util.List;

import com.nlu.shared.domain.model.ApiResponse;

import com.nlu.recruitment.api.dto.JobCardView;
import com.nlu.recruitment.api.dto.JobMatchView;
import com.nlu.recruitment.api.dto.JobDetailView;
import com.nlu.recruitment.api.dto.JobFilterDto;
import com.nlu.recruitment.infrastructure.cache.CategoryCacheService;
import com.nlu.recruitment.application.JobQueryService;
import com.nlu.recruitment.application.JobService;
import com.nlu.shared.utils.MessageUtils;
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
    private final CategoryCacheService categoryCacheService;

    @GetMapping("/newest")
    public ResponseEntity<ApiResponse<Page<JobCardView>>> getListJobNewest(
            @RequestParam(defaultValue = "0") int page, 
            @RequestParam(defaultValue = "10") int size) {
        Page<JobCardView> data = jobQueryService.getNewestJobs(page, size);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Integer>> getAmount() {
        Integer data = jobQueryService.countJobs();
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @PostMapping("/filter")
    public ResponseEntity<ApiResponse<Page<JobCardView>>> filterWithAddressTimeSalary(@RequestBody JobFilterDto jobFilterDTO) {
        Page<JobCardView> data = jobQueryService.findJobsBySalaryAddressAndEmploymentTypes(
                jobFilterDTO.getPageIndex(),
                jobFilterDTO.getPageSize(),
                jobFilterDTO.getMin(),
                jobFilterDTO.getMax(),
                jobFilterDTO.getAddress(),
                jobFilterDTO.getTimes(),
                jobFilterDTO.getTitle(),
                jobFilterDTO.getCategoryIds());
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
    public ResponseEntity<ApiResponse<List<JobMatchView>>> getMatchedJobs(@PathVariable("cvId") long cvId) {
        List<JobMatchView> jobs = jobService.matchJobs(cvId);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), jobs, HttpStatus.OK.value()));
    }
}
