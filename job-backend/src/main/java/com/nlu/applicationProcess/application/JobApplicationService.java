package com.nlu.applicationProcess.application;

import com.nlu.applicationProcess.api.dto.req.ApplyCvWithExistingRequest;
import com.nlu.applicationProcess.api.dto.req.ApplyCvWithUploadRequest;
import com.nlu.applicationProcess.api.dto.req.CandidateDTO;
import com.nlu.identity.domain.model.User;
import org.springframework.data.domain.Page;

import java.io.IOException;


public interface JobApplicationService {
    void applyWithExistingCv(ApplyCvWithExistingRequest request, User user);
    void applyWithUploadCv(ApplyCvWithUploadRequest request, User user) throws IOException;
    Page<CandidateDTO> getCandidatesAppliedToJob(int pageIndex, int pageSize, long jobId);
    Boolean hasApplied(String email, long jobId);
}
