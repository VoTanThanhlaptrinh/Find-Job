package com.job_web.controller;

import com.job_web.dto.AddressJobCount;
import com.job_web.dto.JobDTO;
import com.job_web.dto.JobFilterDTO;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.job_web.dto.ApiResponse;
import com.job_web.models.Job;
import com.job_web.service.JobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;


@RestController
@RequestMapping(path = "/api/job")
@CrossOrigin(origins = "**")
@Slf4j
@RequiredArgsConstructor
public class JobController {
	private final JobService jobService;
	@GetMapping("/pub/listJobsNewest/{pageIndex}/{pageSize}")
	public ResponseEntity<ApiResponse<Page<Job>>> getListJobNewest(@PathVariable int pageIndex, @PathVariable int pageSize) {
		ApiResponse<Page<Job>> res = jobService.getListJobNewest(pageIndex,pageSize);
		return ResponseEntity.status(res.getStatus()).body(res);
	}
	@GetMapping("/pub/detail/{id}")
	public ResponseEntity<ApiResponse<Job>> getMethodName(@PathVariable String id) {
		ApiResponse<Job> res = jobService.getJobDetailById(id);
		return ResponseEntity.status(res.getStatus()).body(res);
	}
	@GetMapping("/pub/check-apply/{id}")
	public ResponseEntity<ApiResponse<Boolean>> checkApplyJob(@PathVariable String id) {
		ApiResponse<Boolean> res = jobService.checkExistJob(id);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@GetMapping("/pub/getAmount")
	public ResponseEntity<ApiResponse<Integer>> getAmount() {
		ApiResponse<Integer> res = jobService.getAmount();
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@GetMapping("/pub/getAddressCount")
	public ResponseEntity<ApiResponse<List<AddressJobCount>>> getAddressCount() {
		ApiResponse<List<AddressJobCount>> res = jobService.getAddressJobCount();
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@PostMapping("/pub/filterWithAddressTimeSalary")
	public ResponseEntity<ApiResponse<Page<Job>>> filterWithAddressTimeSalary(@RequestBody JobFilterDTO jobFilterDTO) {
		ApiResponse<Page<Job>> res = jobService.filterBetterSalaryAndHasAddressAndInTimes(jobFilterDTO.getPageIndex(),jobFilterDTO.getPageSize(),jobFilterDTO.getMin(),jobFilterDTO.getMax(),jobFilterDTO.getAddress(),jobFilterDTO.getTimes());
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@PostMapping(value = "/pri/postJob", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<String>> post( @Valid @ModelAttribute JobDTO job, BindingResult bindingResult, Principal principal) {
		if(bindingResult.hasErrors()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(bindingResult.getAllErrors().getFirst().getDefaultMessage(),null,HttpStatus.BAD_REQUEST.value()));
		}
		ApiResponse<String> res = jobService.saveJob(job.toJob(),principal);
		return ResponseEntity.status(res.getStatus()).body(res);
	}
}
