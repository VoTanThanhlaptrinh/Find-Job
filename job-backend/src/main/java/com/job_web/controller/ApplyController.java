package com.job_web.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping(path = "/api/apply", produces = "application/json")
public class ApplyController {
	
	@PostMapping("/submit")
	public ResponseEntity<Map<String, String>> postSubmitCV(@RequestBody Map<String, Object> req) {
		// TODO: process POST request
		Map<String, String> res = new HashMap<>();
		return ResponseEntity.ok(res);
	}

}
