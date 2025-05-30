package com.job_web.api;

import com.job_web.dto.ApplyDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.job_web.dto.ApiResponse;
import com.job_web.models.Job;
import com.job_web.service.JobService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;


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

	@PostMapping(value = "/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ApiResponse<String>> ApplyCV(@ModelAttribute ApplyDTO applyDTO, BindingResult bindingResult) throws IOException {
		if(bindingResult.hasErrors()) {
			return ResponseEntity.badRequest().body(new ApiResponse<String>(bindingResult.getAllErrors().getFirst().getDefaultMessage(),null,400));
		}
		ApiResponse<String> res = jobService.apply(applyDTO.getJobId(),applyDTO.getFile().getBytes(),applyDTO.getFile().getName());
		return ResponseEntity.status(res.getStatus()).body(res);
	}
}
