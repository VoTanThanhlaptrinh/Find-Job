package com.job_web.service.application.impl;

import com.job_web.data.ApplyRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.ResumeRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.ai.ResumeParsingMessage;
import com.job_web.dto.application.ApplyCvWithExistingRequest;
import com.job_web.dto.application.ApplyCvWithUploadRequest;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.application.CandidateDTO;
import com.job_web.dto.message.CloudUploadMessage;
import com.job_web.message.MessageProducer;
import com.job_web.models.Apply;
import com.job_web.models.Job;
import com.job_web.models.Resume;
import com.job_web.models.User;
import com.job_web.service.application.ApplyService;
import com.job_web.service.application.ResumeService;
import com.job_web.service.support.FileService;
import com.job_web.utills.KeyGeneratorUtil;
import com.job_web.utills.MessageUtils;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ApplyServiceImpl implements ApplyService {
    private final ApplyRepository applyRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ResumeService resumeService;
    private final MessageProducer messageProducer;
    private final FileService fileService;

    @Transactional(rollbackFor = {Exception.class, Throwable.class})
    @Override
    public ApiResponse<String> applyWithExistingCv(ApplyCvWithExistingRequest request, User user) {
        var currentUser = userRepository.findByEmail(user.getEmail());
        if(currentUser.isEmpty()){
            return new ApiResponse<>(MessageUtils.getMessage("auth.user.not_found"), null, HttpStatus.BAD_REQUEST.value());
        }
        var job = jobRepository.findById(request.getJobId());
        if(job.isEmpty()){
            return new ApiResponse<>(MessageUtils.getMessage("job.not_found"), null, HttpStatus.BAD_REQUEST.value());
        }
        var resume = resumeRepository.findById(request.getExistingCvId());
        if(resume.isEmpty()){
            return new ApiResponse<>(MessageUtils.getMessage("resume.not_found"), null, HttpStatus.BAD_REQUEST.value());
        }
        if(applyRepository.findByJobAndUser(currentUser.get().getEmail(), request.getJobId()).isPresent()){
            return new ApiResponse<>(MessageUtils.getMessage("application.already_applied"), null, HttpStatus.BAD_REQUEST.value());
        }
        if(resumeRepository.countOwnedByUser(request.getExistingCvId(), currentUser.get().getEmail()) == 0){
            return new ApiResponse<>(MessageUtils.getMessage("resume.not_owned"), null, HttpStatus.BAD_REQUEST.value());
        }
        Apply apply = new Apply();
        apply.setJob(job.get());
        apply.setResume(resume.get());
        apply.setUser(currentUser.get());
        apply.setApplyDate(LocalDateTime.now());
        applyRepository.save(apply);
        return new ApiResponse<>(MessageUtils.getMessage("application.success"), null, HttpStatus.OK.value());
    }

    @Transactional(rollbackFor = {Exception.class, Throwable.class})
    @Override
    public ApiResponse<String> applyWithUploadCv(ApplyCvWithUploadRequest request, User user) throws IOException {
        var currentUser = userRepository.findByEmail(user.getEmail());
        if(currentUser.isEmpty()){
            return new ApiResponse<>(MessageUtils.getMessage("auth.user.not_found"), null, HttpStatus.BAD_REQUEST.value());
        }
        if(applyRepository.findByJobAndUser(currentUser.get().getEmail(), request.getJobId()).isPresent()){
            return new ApiResponse<>(MessageUtils.getMessage("application.already_applied"), null, HttpStatus.BAD_REQUEST.value());
        }
        var job = jobRepository.findById(request.getJobId());
        if(job.isEmpty()){
            return new ApiResponse<>(MessageUtils.getMessage("job.not_found"), null, HttpStatus.BAD_REQUEST.value());
        }
        if(resumeRepository.countActiveByUserEmail(currentUser.get().getEmail()) >= 5){
            return new ApiResponse<>(MessageUtils.getMessage("resume.max_count"), null, HttpStatus.BAD_REQUEST.value());
        }
        try{
            var resume = new Resume();
            resume.setUser(currentUser.get());
            resume.setCreateDate(LocalDateTime.now());

            String key = KeyGeneratorUtil.generateKey();
            resume.setKeyCf(key);
            resume.setFileName(request.getCvFile().getOriginalFilename());

            byte[] data = resumeService.toByteArray(request.getCvFile().getInputStream());
            String rawText = fileService.extractTextFromFile(request.getCvFile().getInputStream());

            resumeRepository.save(resume);

            // Đẩy upload và AI processing vào message queue
            messageProducer.uploadToCloud(new CloudUploadMessage(data, key, request.getCvFile().getOriginalFilename()));
            messageProducer.processAI(new ResumeParsingMessage(rawText, currentUser.get().getId(), resume.getId()));

            Apply apply = new Apply();
            apply.setJob(job.get());
            apply.setResume(resume);
            apply.setUser(currentUser.get());
            apply.setApplyDate(LocalDateTime.now());

            applyRepository.save(apply);
            return new ApiResponse<>(MessageUtils.getMessage("application.success"), null, HttpStatus.OK.value());
        }catch (Exception e){
            throw new RuntimeException(MessageUtils.getMessage("error.system", e.getMessage()), e);
        }
    }

    @Override
    public ApiResponse<Page<CandidateDTO>> getAllCandidateAppliedJob(int pageIndex, int pageSize, long jobId) {
        Page<CandidateDTO> page = applyRepository.getAllCandidateAppliedJob(jobId,PageRequest.of(pageIndex,pageSize));
        int status = page.isEmpty() ? HttpStatus.NOT_FOUND.value()  : HttpStatus.OK.value();
        String message = status == 200 ? MessageUtils.getMessage("message.success") : MessageUtils.getMessage("message.not_found");
        return new ApiResponse<>(message,page,status);
    }

    @Override
    public ApiResponse<Boolean> hasApplied(String email, long jobId) {
        Optional<Apply> optionalApply = applyRepository.findByJobAndUser(email, jobId);
        return new ApiResponse<>(MessageUtils.getMessage("message.success"), optionalApply.isPresent(), HttpStatus.OK.value());
    }
}
