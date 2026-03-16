package com.job_web.service.application;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.CandidateDTO;
import com.job_web.models.Job;
import com.job_web.models.User;
import org.springframework.data.domain.Page;

import java.security.Principal;


public interface ApplyService {
	public void submit(User user, Job job);
    public ApiResponse<Page<CandidateDTO>> getAllCandidateAppliedJob(int pageIndex, int pageSize, long jobId);
    public ApiResponse<Boolean> hasApplied(Principal principal, long jobId);
}



