package com.job_web.service.application.impl;

import com.job_web.data.ApplyRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.CandidateDTO;
import com.job_web.models.Apply;
import com.job_web.models.Job;
import com.job_web.models.User;
import com.job_web.service.application.ApplyService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ApplyServiceImpl implements ApplyService {
    private final ApplyRepository applyRepository;

    @Override
    public void submit(User user, Job job) {

    }

    @Override
    public ApiResponse<Page<CandidateDTO>> getAllCandidateAppliedJob(int pageIndex, int pageSize, long jobId) {
        Page<CandidateDTO> page = applyRepository.getAllCandidateAppliedJob(jobId,PageRequest.of(pageIndex,pageSize));
        int status = page.isEmpty() ? HttpStatus.NOT_FOUND.value()  : HttpStatus.OK.value();
        String message = status == 200 ? "success" : "not found";
        return new ApiResponse<>(message,page,status);
    }

    @Override
    public ApiResponse<Boolean> hasApplied(Principal principal, long jobId) {
        Optional<Apply> optionalApply = applyRepository.findByJobAndUser(principal.getName(), jobId);
        return new ApiResponse<>("success", optionalApply.isPresent(), HttpStatus.OK.value());
    }
}



