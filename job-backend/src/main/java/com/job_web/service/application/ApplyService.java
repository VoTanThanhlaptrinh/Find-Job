package com.job_web.service.application;

import com.job_web.dto.application.ApplyCvWithExistingRequest;
import com.job_web.dto.application.ApplyCvWithUploadRequest;
import com.job_web.dto.application.CandidateDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.models.User;
import org.springframework.data.domain.Page;

import java.io.IOException;


public interface ApplyService {
    ApiResponse<String> applyWithExistingCv(ApplyCvWithExistingRequest request, User user);
    ApiResponse<String> applyWithUploadCv(ApplyCvWithUploadRequest request, User user) throws IOException;
    ApiResponse<Page<CandidateDTO>> getAllCandidateAppliedJob(int pageIndex, int pageSize, long jobId);
    ApiResponse<Boolean> hasApplied(String email, long jobId);
}
