package com.job_web.service.application.impl;

import com.job_web.data.ApplyRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.ResumeRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.application.ApplyCvWithExistingRequest;
import com.job_web.dto.application.ApplyCvWithUploadRequest;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.CandidateDTO;
import com.job_web.models.Apply;
import com.job_web.models.Job;
import com.job_web.models.Resume;
import com.job_web.models.User;
import com.job_web.service.application.ApplyService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ApplyServiceImpl implements ApplyService {
    private final ApplyRepository applyRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @Override
    public ApiResponse<String> applyWithExistingCv(ApplyCvWithExistingRequest request, Principal principal) {

        var job = jobRepository.findById(request.getJobId());
        if(job.isEmpty()){
            return new ApiResponse<>("Công việc không tồn tại.", null, HttpStatus.BAD_REQUEST.value());
        }
        var resume = resumeRepository.findById(request.getExistingCvId());
        if(resume.isEmpty()){
            return new ApiResponse<>("CV không tồn tại.", null, HttpStatus.BAD_REQUEST.value());
        }
        var user = userRepository.findByEmail(principal.getName());
        if(user.isEmpty()){
            return new ApiResponse<>("Người dùng không tồn tại.", null, HttpStatus.BAD_REQUEST.value());
        }
        if(user.get().getApplies().stream().anyMatch(apply -> apply.getJob().getId() == request.getJobId())){
            return new ApiResponse<>("Người dùng đã apply", null, HttpStatus.BAD_REQUEST.value());
        }
        if(!resume.get().getUser().getEmail().equals(principal.getName())){
            return new ApiResponse<>("CV không thuộc user", null, HttpStatus.BAD_REQUEST.value());
        }
        Apply apply = new Apply();
        apply.setJob(job.get());
        apply.setResume(resume.get());
        apply.setUser(user.get());
        apply.setApplyDate(LocalDateTime.now());
        applyRepository.save(apply);
        return new ApiResponse<>("Ứng tuyền thành công", null, HttpStatus.OK.value());
    }
    @Transactional(rollbackFor = {Exception.class, Throwable.class})
    @Override
    public ApiResponse<String> applyWithUploadCv(ApplyCvWithUploadRequest request, Principal principal) throws IOException {
        var user = userRepository.findByEmail(principal.getName());
        if(user.isEmpty()){
            return new ApiResponse<>("Người dùng không tồn tại.", null, HttpStatus.BAD_REQUEST.value());
        }
        if(user.get().getApplies().stream().anyMatch(apply -> apply.getJob().getId() == request.getJobId())){
            return new ApiResponse<>("Người dùng đã apply", null, HttpStatus.BAD_REQUEST.value());
        }
        var job = jobRepository.findById(request.getJobId());
        if(job.isEmpty()){
            return new ApiResponse<>("Công việc không tồn tại.", null, HttpStatus.BAD_REQUEST.value());
        }
        if(user.get().getResumes().size() > 5){
            return new ApiResponse<>("Người dùng chỉ được phép có tối đa 5 cái cv.", null, HttpStatus.BAD_REQUEST.value());
        }
        try{
            var resume = new Resume();
            resume.setUser(user.get());
            resume.setCreateDate(LocalDateTime.now());
            resume.setData(request.getCvFile().getBytes());
            resume.setFileName(request.getCvFile().getName());
            resumeRepository.save(resume);

            Apply apply = new Apply();
            apply.setJob(job.get());
            apply.setResume(resume);
            apply.setUser(user.get());
            apply.setApplyDate(LocalDateTime.now());

            applyRepository.save(apply);
            return new ApiResponse<>("Not implemented.", null, HttpStatus.NOT_IMPLEMENTED.value());
        }catch (Exception e){
            return new ApiResponse<>("Lỗi hệ thống", null, HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
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



