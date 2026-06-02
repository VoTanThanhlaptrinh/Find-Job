package com.job_web.application_process.application;

import com.job_web.application_process.api.dto.ApplyCvWithExistingRequest;
import com.job_web.application_process.api.dto.ApplyCvWithUploadRequest;
import com.job_web.application_process.api.dto.CandidateDTO;
import com.job_web.identity.domain.model.User;
import org.springframework.data.domain.Page;

import java.io.IOException;


public interface JobApplicationService {
    void applyWithExistingCv(ApplyCvWithExistingRequest request, User user);
    void applyWithUploadCv(ApplyCvWithUploadRequest request, User user) throws IOException;
    Page<CandidateDTO> getAllCandidateAppliedJob(int pageIndex, int pageSize, long jobId);
    Boolean hasApplied(String email, long jobId);
}
