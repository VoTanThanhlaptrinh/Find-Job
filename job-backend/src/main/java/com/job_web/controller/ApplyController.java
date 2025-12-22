package com.job_web.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import com.job_web.dto.ApiResponse;
import com.job_web.dto.CandidateDTO;
import com.job_web.service.ApplyService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/apply", produces = "application/json")
@AllArgsConstructor
public class ApplyController {
	private final ApplyService applyService;

	@PostMapping("/submit")
	public ResponseEntity<Map<String, String>> postSubmitCV(@RequestBody Map<String, Object> req) {
		// TODO: process POST request
		Map<String, String> res = new HashMap<>();
		return ResponseEntity.ok(res);
	}
    @GetMapping("/pri/h/getAllCandidateAppliedJob/{pageIndex}/{pageSize}/{jobId}")
    public ResponseEntity<ApiResponse<Page<CandidateDTO>>> getAllCandidateAppliedJob(@PathVariable("pageIndex") int pageIndex
            , @PathVariable("pageSize") int pageSize, long jobId){
        ApiResponse<Page<CandidateDTO>> res = applyService.getAllCandidateAppliedJob(pageIndex,pageSize,jobId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
    @GetMapping("/pri/u/hasApplied/{jobId}")
    public ResponseEntity<ApiResponse<Boolean>> hasApplied(@PathVariable("jobId") long jobId, Principal principal){
        ApiResponse<Boolean> res = applyService.hasApplied(principal,jobId);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
