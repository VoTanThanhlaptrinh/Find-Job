package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.domain.repository.JobApplicationRepository;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.req.ApplyCvWithExistingRequest;
import com.nlu.applicationProcess.api.dto.req.ApplyCvWithUploadRequest;
import com.nlu.applicationProcess.api.dto.req.CandidateDTO;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.infrastructure.message.MessageProducer;
import com.nlu.applicationProcess.domain.model.JobApplication;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.recruitment.domain.model.Job;
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
    private final CloudStorageService cloudStorageService;

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

            // 1. Common Validation
            ApplicationContext context = validateAndGetApplicationContext(user.getEmail(), request.jobId());

            // 2. Specific Validation for Existing CV
            var resume = resumeRepository.findById(request.existingCvId())
                    .orElseThrow(() -> new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found")));

            if (resumeRepository.countOwnedByUser(request.existingCvId(), context.currentUser().getEmail()) == 0) {
                log.warn("CV ownership violation — user: {} does not own CV: {}",
                        context.currentUser().getId(), request.existingCvId());
                throw new BadRequestException(MessageUtils.getMessage("resume.not_owned"));
            }

            // 3. Save Application
            createAndSaveJobApplication(context.job(), resume, context.currentUser());

            log.info("Application completed — user: {} applied to job: {} with existing CV: {}",
                    context.currentUser().getId(), request.jobId(), request.existingCvId());

        } finally {
            clearMDC();
        }
    }

    @Transactional(rollbackFor = { Exception.class, Throwable.class })
    @Override
    public void applyWithUploadCv(ApplyCvWithUploadRequest request, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_JOB_ID, String.valueOf(request.getJobId()));

            String originalFilename = request.getCvFile().getOriginalFilename();
            log.info("Processing job application with CV upload for user: {}, job: {}, file: {}",
                    user.getId(), request.getJobId(), originalFilename);

            // 1. Common Validation
            ApplicationContext context = validateAndGetApplicationContext(user.getEmail(), request.getJobId());

            // 2. Quota Check
            long activeResumeCount = resumeRepository.countActiveByUserEmail(context.currentUser().getEmail());
            if (activeResumeCount >= 5) {
                log.warn("Resume quota exceeded — user: {} has {} active resumes (max: 5)",
                        context.currentUser().getId(), activeResumeCount);
                throw new BadRequestException(MessageUtils.getMessage("resume.max_count"));
            }

            // 3. Process File (Extract bytes and text)
            byte[] data;
            String rawText;
            try {
                data = fileService.toByteArray(request.getCvFile().getInputStream());
                rawText = fileService.extractTextFromFile(request.getCvFile().getInputStream());

                // Optional: Add OCR fallback here if fileService supports it, similar to your createResume logic
                if (rawText == null || rawText.isEmpty()) {
                    log.info("Standard text extraction empty, attempting OCR for user: {}", user.getId());
                    // rawText = fileService.extractTextFromFileOcr(request.getCvFile());
                }
                rawText = fileService.cleanText(rawText);
            } catch (Exception e) {
                log.error("Failed to read or parse uploaded CV file for user: {}", user.getId(), e);
                throw new BadRequestException(MessageUtils.getMessage("resume.upload.failed"));
            }

            if (data.length == 0) {
                log.warn("Empty file uploaded during resume creation for user: {}", user.getId());
                throw new BadRequestException(MessageUtils.getMessage("message.error")); // Or a more specific "empty file" message
            }

            // 4. SYNCHRONOUS Cloud Upload
            String key = KeyGeneratorUtil.generateKey();
            try {
                // Ensure this method in CloudStorageService is a blocking/synchronous call
                cloudStorageService.uploadFile(data, key, originalFilename);
                log.info("Successfully uploaded file to Cloudflare R2 for user: {}, key: {}", user.getId(), key);
            } catch (Exception e) {
                log.error("Cloud storage upload failed for user: {}. Aborting application.", user.getId(), e);
                // Throwing here triggers @Transactional rollback. Nothing is saved to DB.
                throw new RuntimeException("Tải file lên cloud thất bại, vui lòng thử lại sau.");
            }

            // 5. Save Entities (Only executed if upload succeeds)
            var resume = new Resume();
            resume.setUser(context.currentUser());
            resume.setCreateDate(LocalDateTime.now());
            resume.setKeyCf(key);
            resume.setFileName(originalFilename);

            resumeRepository.save(resume);
            MDC.put(MDC_CV_ID, String.valueOf(resume.getId()));

            createAndSaveJobApplication(context.job(), resume, context.currentUser());

            // 6. Trigger Asynchronous AI Processing
            messageProducer.processAI(new ResumeParsingMessage(rawText, context.currentUser().getId(), resume.getId()));

            log.info("Application completed — user: {} applied to job: {} with new CV: {}. Dispatched AI processing.",
                    context.currentUser().getId(), request.getJobId(), resume.getId());
        } finally {
            clearMDC();
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
        return jobApplicationRepository.findByJobAndUser(email, jobId).isPresent();
    }

    // --- Helper Methods ---

    /**
     * Centralizes common validation for finding user, finding job, and checking duplicate applications.
     */
    private ApplicationContext validateAndGetApplicationContext(String email, long jobId) {
        User currentUser = userRepository.findByEmail_Value(email)
                .orElseThrow(() -> new ResourceNotFoundException(MessageUtils.getMessage("auth.user.not_found")));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(MessageUtils.getMessage("job.not_found")));

        if (jobApplicationRepository.findByJobAndUser(currentUser.getEmail(), jobId).isPresent()) {
            log.warn("Duplicate application blocked — user: {} already applied to job: {}", currentUser.getId(), jobId);
            throw new BadRequestException(MessageUtils.getMessage("application.already_applied"));
        }

        return new ApplicationContext(currentUser, job);
    }

    private void createAndSaveJobApplication(Job job, Resume resume, User user) {
        JobApplication jobApplication = new JobApplication();
        jobApplication.setJob(job);
        jobApplication.setResume(resume);
        jobApplication.setUser(user);
        jobApplication.setApplyDate(LocalDateTime.now());
        jobApplicationRepository.save(jobApplication);
    }

    private void clearMDC() {
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_JOB_ID);
        MDC.remove(MDC_CV_ID);
    }

    // Simple record to pass multiple validated entities back from the helper method
    private record ApplicationContext(User currentUser, Job job) {}
}