package com.job_web.service;

import com.job_web.dto.ApiResponse;
import com.job_web.dto.CandidateDTO;
import com.job_web.models.Job;
import com.job_web.models.User;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.security.Principal;


public interface ApplyService {
	public void submit(User user, Job job);
    public ApiResponse<Page<CandidateDTO>> getAllCandidateAppliedJob(int pageIndex, int pageSize, long jobId);
    public ApiResponse<Boolean> hasApplied(Principal principal, long jobId);
}
