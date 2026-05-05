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
import com.job_web.exception.BadRequestException;
import com.job_web.exception.ResourceNotFoundException;
import com.job_web.message.MessageProducer;
import com.job_web.models.Apply;
import com.job_web.models.Resume;
import com.job_web.models.User;
import com.job_web.service.application.ApplyService;
import com.job_web.service.application.ResumeService;
import com.job_web.service.support.FileService;
import com.job_web.utils.KeyGeneratorUtil;
import com.job_web.utils.MessageUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jdt.core.compiler.InvalidInputException;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
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

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_JOB_ID = "jobId";
    private static final String MDC_CV_ID = "cvId";

    @Transactional(rollbackFor = { Exception.class, Throwable.class })
    @Override
    public void applyWithExistingCv(ApplyCvWithExistingRequest request, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_JOB_ID, String.valueOf(request.jobId()));
            MDC.put(MDC_CV_ID, String.valueOf(request.existingCvId()));

            log.info("Processing job application with existing CV for user: {}, job: {}, cv: {}",
                    user.getId(), request.jobId(), request.existingCvId());

            var currentUser = userRepository.findByEmail(user.getEmail());
            if (currentUser.isEmpty()) {
                throw new ResourceNotFoundException(MessageUtils.getMessage("auth.user.not_found"));
            }
            var job = jobRepository.findById(request.jobId());
            if (job.isEmpty()) {
                throw new ResourceNotFoundException(MessageUtils.getMessage("job.not_found"));
            }
            var resume = resumeRepository.findById(request.existingCvId());
            if (resume.isEmpty()) {
                throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
            }
            if (applyRepository.findByJobAndUser(currentUser.get().getEmail(), request.jobId()).isPresent()) {
                log.warn("Duplicate application blocked — user: {} already applied to job: {}",
                        currentUser.get().getId(), request.jobId());
                throw new BadRequestException(MessageUtils.getMessage("application.already_applied"));
            }
            if (resumeRepository.countOwnedByUser(request.existingCvId(), currentUser.get().getEmail()) == 0) {
                log.warn("CV ownership violation — user: {} does not own CV: {}",
                        currentUser.get().getId(), request.existingCvId());
                throw new BadRequestException(MessageUtils.getMessage("resume.not_owned"));
            }

            Apply apply = new Apply();
            apply.setJob(job.get());
            apply.setResume(resume.get());
            apply.setUser(currentUser.get());
            apply.setApplyDate(LocalDateTime.now());
            applyRepository.save(apply);

            log.info("Application completed — user: {} applied to job: {} with existing CV: {}",
                    currentUser.get().getId(), request.jobId(), request.existingCvId());

        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_JOB_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Transactional(rollbackFor = { Exception.class, Throwable.class })
    @Override
    public void applyWithUploadCv(ApplyCvWithUploadRequest request, User user) throws IOException {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_JOB_ID, String.valueOf(request.getJobId()));

            log.info("Processing job application with CV upload for user: {}, job: {}, file: {}",
                    user.getId(), request.getJobId(), request.getCvFile().getOriginalFilename());

            var currentUser = userRepository.findByEmail(user.getEmail());
            if (currentUser.isEmpty()) {
                throw new ResourceNotFoundException(MessageUtils.getMessage("auth.user.not_found"));
            }
            if (applyRepository.findByJobAndUser(currentUser.get().getEmail(), request.getJobId()).isPresent()) {
                log.warn("Duplicate application blocked — user: {} already applied to job: {}",
                        currentUser.get().getId(), request.getJobId());
                throw new BadRequestException(MessageUtils.getMessage("application.already_applied"));
            }
            var job = jobRepository.findById(request.getJobId());
            if (job.isEmpty()) {
                throw new ResourceNotFoundException(MessageUtils.getMessage("job.not_found"));
            }
            long activeResumeCount = resumeRepository.countActiveByUserEmail(currentUser.get().getEmail());
            if (activeResumeCount >= 5) {
                log.warn("Resume quota exceeded — user: {} has {} active resumes (max: 5)",
                        currentUser.get().getId(), activeResumeCount);
                throw new BadRequestException(MessageUtils.getMessage("resume.max_count"));
            }

            var resume = new Resume();
            resume.setUser(currentUser.get());
            resume.setCreateDate(LocalDateTime.now());

            String key = KeyGeneratorUtil.generateKey();
            resume.setKeyCf(key);
            resume.setFileName(request.getCvFile().getOriginalFilename());

            byte[] data = resumeService.toByteArray(request.getCvFile().getInputStream());
            String rawText = fileService.extractTextFromFile(request.getCvFile().getInputStream());

            resumeRepository.save(resume);
            MDC.put(MDC_CV_ID, String.valueOf(resume.getId()));

            messageProducer.uploadToCloud(new CloudUploadMessage(data, key, request.getCvFile().getOriginalFilename()));
            messageProducer.processAI(new ResumeParsingMessage(rawText, currentUser.get().getId(), resume.getId()));
            log.info("Dispatched cloud upload and AI processing for CV: {}", resume.getId());

            Apply apply = new Apply();
            apply.setJob(job.get());
            apply.setResume(resume);
            apply.setUser(currentUser.get());
            apply.setApplyDate(LocalDateTime.now());

            applyRepository.save(apply);

            log.info("Application completed — user: {} applied to job: {} with new CV: {}",
                    currentUser.get().getId(), request.getJobId(), resume.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_JOB_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Override
    public Page<CandidateDTO> getAllCandidateAppliedJob(int pageIndex, int pageSize, long jobId) {
        Page<CandidateDTO> page = applyRepository.getAllCandidateAppliedJob(jobId, PageRequest.of(pageIndex, pageSize));
        if (page.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("message.not_found"));
        }
        return page;
    }

    @Override
    public Boolean hasApplied(String email, long jobId) {
        Optional<Long> optionalApply = applyRepository.findByJobAndUser(email, jobId);
        return optionalApply.isPresent();
    }
}
