package com.job_web.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.job_web.dto.ApiResponse;
import com.job_web.models.Job;
import com.job_web.service.JobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;




@RestController
@RequestMapping(path = "/api/job", produces = "application/json")
@CrossOrigin(origins = "**")
@Slf4j
@RequiredArgsConstructor
public class JobAPI {
	@Value("${application.service.impl.amount}")
	private int amount;
	private final JobService jobService;
	@GetMapping("/listJobsNewest/{page}")
	public ResponseEntity<ApiResponse<Page<Job>>> getListJobNewest(@PathVariable int page) {
		ApiResponse<Page<Job>> res = jobService.getListJobNewest(page,amount);
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
	
}
