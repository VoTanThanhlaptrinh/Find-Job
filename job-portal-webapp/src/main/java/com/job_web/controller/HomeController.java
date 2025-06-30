package com.job_web.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.job_web.data.BlogRepository;
import com.job_web.data.JobRepository;
import com.job_web.dto.ApiResponse;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api")
@AllArgsConstructor

public class HomeController {
	private JobRepository jobRepository;
	private BlogRepository blogRepository;
	@GetMapping("/home/init")
	public ResponseEntity<ApiResponse<Object>> getInit() {
		Map<String, Object> response = new HashMap<>();
		PageRequest jopBySalary = PageRequest.of(0, 7, Sort.by("salary").descending());
		PageRequest topJobBy = PageRequest.of(0, 5, Sort.by("createDate").ascending());
		PageRequest blogByTime = PageRequest.of(0, 3, Sort.by("amountLike").descending());
		try {
			 response.put("jobSalary", jobRepository.findAll(jopBySalary));
			 response.put("jobSoon", jobRepository.findAll(topJobBy));
//			 response.put("blog", blogRepository.findAll(blogByTime));
			 return ResponseEntity.ok(new ApiResponse<>("Load dữ liệu thành công", response, 200));
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(new ApiResponse<>(e.getMessage(), null, 200));
		}
	}
	
}
