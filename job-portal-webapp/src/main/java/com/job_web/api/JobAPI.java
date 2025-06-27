package com.job_web.api;

import com.job_web.dto.AddressJobCount;
import com.job_web.dto.JobFilterDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.job_web.dto.ApiResponse;
import com.job_web.models.Job;
import com.job_web.service.JobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


@RestController
@RequestMapping(path = "/api/job/pub", produces = "application/json")
@CrossOrigin(origins = "**")
@Slf4j
@RequiredArgsConstructor
public class JobAPI {
	private final JobService jobService;
	@GetMapping("/listJobsNewest/{pageIndex}/{pageSize}")
	public ResponseEntity<ApiResponse<Page<Job>>> getListJobNewest(@PathVariable int pageIndex, @PathVariable int pageSize) {
		ApiResponse<Page<Job>> res = jobService.getListJobNewest(pageIndex,pageSize);
		return ResponseEntity.status(res.getStatus()).body(res);
	}
	@GetMapping("/detail/{id}")
	public ResponseEntity<ApiResponse<Job>> getMethodName(@PathVariable String id) {
		ApiResponse<Job> res = jobService.getJobDetailById(id);
		return ResponseEntity.status(res.getStatus()).body(res);
	}
	@GetMapping("/check-apply/{id}")
	public ResponseEntity<ApiResponse<Boolean>> checkApplyJob(@PathVariable String id) {
		ApiResponse<Boolean> res = jobService.checkExistJob(id);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@GetMapping("/getAmount")
	public ResponseEntity<ApiResponse<Integer>> getAmount() {
		ApiResponse<Integer> res = jobService.getAmount();
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@GetMapping("/getAddressCount")
	public ResponseEntity<ApiResponse<List<AddressJobCount>>> getAddressCount() {
		ApiResponse<List<AddressJobCount>> res = jobService.getAddressJobCount();
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@PostMapping("/filterWithAddressTimeSalary")
	public ResponseEntity<ApiResponse<Page<Job>>> filterWithAddressTimeSalary(@RequestBody JobFilterDTO jobFilterDTO) {
		ApiResponse<Page<Job>> res = jobService.filterBetterSalaryAndHasAddressAndInTimes(jobFilterDTO.getPageIndex(),jobFilterDTO.getPageSize(),jobFilterDTO.getMin(),jobFilterDTO.getMax(),jobFilterDTO.getAddress(),jobFilterDTO.getTimes());
		return ResponseEntity.status(res.getStatus()).body(res);
	}
}
