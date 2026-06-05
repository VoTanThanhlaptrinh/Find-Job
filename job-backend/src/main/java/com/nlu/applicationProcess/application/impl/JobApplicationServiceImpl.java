package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.domain.repository.JobApplicationRepository;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.req.ApplyCvWithExistingRequest;
import com.nlu.applicationProcess.api.dto.req.ApplyCvWithUploadRequest;

import com.nlu.applicationProcess.api.dto.req.CandidateDTO;
import com.nlu.shared.api.message.dto.CloudUploadMessage;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.infrastructure.message.MessageProducer;
import com.nlu.applicationProcess.domain.model.JobApplication;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.identity.domain.model.User;
import com.nlu.applicationProcess.application.JobApplicationService;
import com.nlu.shared.application.FileService;
import com.nlu.shared.utils.KeyGeneratorUtil;
import com.nlu.shared.utils.MessageUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@AllArgsConstructor
public class JobApplicationServiceImpl implements JobApplicationService {
    private final JobApplicationRepository jobApplicationRepository;
    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
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

            var currentUser = userRepository.findByEmail_Value(user.getEmail());
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
            if (jobApplicationRepository.findByJobAndUser(currentUser.get().getEmail(), request.jobId()).isPresent()) {
                log.warn("Duplicate application blocked — user: {} already applied to job: {}",
                        currentUser.get().getId(), request.jobId());
                throw new BadRequestException(MessageUtils.getMessage("application.already_applied"));
            }
            if (resumeRepository.countOwnedByUser(request.existingCvId(), currentUser.get().getEmail()) == 0) {
                log.warn("CV ownership violation — user: {} does not own CV: {}",
                        currentUser.get().getId(), request.existingCvId());
                throw new BadRequestException(MessageUtils.getMessage("resume.not_owned"));
            }

            JobApplication jobApplication = new JobApplication();
            jobApplication.setJob(job.get());
            jobApplication.setResume(resume.get());
            jobApplication.setUser(currentUser.get());
            jobApplication.setApplyDate(LocalDateTime.now());
            jobApplicationRepository.save(jobApplication);

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

            var currentUser = userRepository.findByEmail_Value(user.getEmail());
            if (currentUser.isEmpty()) {
                throw new ResourceNotFoundException(MessageUtils.getMessage("auth.user.not_found"));
            }
            if (jobApplicationRepository.findByJobAndUser(currentUser.get().getEmail(), request.getJobId()).isPresent()) {
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

            byte[] data = fileService.toByteArray(request.getCvFile().getInputStream());
            String rawText = fileService.extractTextFromFile(request.getCvFile().getInputStream());

            resumeRepository.save(resume);
            MDC.put(MDC_CV_ID, String.valueOf(resume.getId()));

            messageProducer.uploadToCloud(new CloudUploadMessage(data, key, request.getCvFile().getOriginalFilename()));
            messageProducer.processAI(new ResumeParsingMessage(rawText, currentUser.get().getId(), resume.getId()));
            log.info("Dispatched cloud upload and AI processing for CV: {}", resume.getId());

            JobApplication jobApplication = new JobApplication();
            jobApplication.setJob(job.get());
            jobApplication.setResume(resume);
            jobApplication.setUser(currentUser.get());
            jobApplication.setApplyDate(LocalDateTime.now());

            jobApplicationRepository.save(jobApplication);

            log.info("Application completed — user: {} applied to job: {} with new CV: {}",
                    currentUser.get().getId(), request.getJobId(), resume.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_JOB_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Override
    public Page<CandidateDTO> getCandidatesAppliedToJob(int pageIndex, int pageSize, long jobId) {
        Page<CandidateDTO> page = jobApplicationRepository.getCandidatesAppliedToJob(jobId, PageRequest.of(pageIndex, pageSize));
        if (page.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("message.not_found"));
        }
        return page;
    }

    @Override
    public Boolean hasApplied(String email, long jobId) {
        Optional<Long> optionalApply = jobApplicationRepository.findByJobAndUser(email, jobId);
        return optionalApply.isPresent();
    }
}
