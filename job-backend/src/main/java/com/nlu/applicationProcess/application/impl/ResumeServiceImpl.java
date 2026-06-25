package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.infrastructure.query.ResumeQueryDSL;
import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.req.ResumeDetailDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeUploadDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeUrlDTO;
import com.nlu.shared.api.message.dto.CloudUploadMessage;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.domain.exception.UnauthorizedException;
import com.nlu.shared.infrastructure.message.MessageProducer;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.identity.domain.model.User;
import com.nlu.applicationProcess.application.ResumeParsingService;
import com.nlu.applicationProcess.application.ResumeService;
import com.nlu.shared.application.FileService;
import com.nlu.shared.application.S3PresignedUrlService;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.domain.model.SseMessagePayload;
import com.nlu.shared.utils.KeyGeneratorUtil;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    private final ResumeQueryDSL resumeQueryDSL;
    private final FileService fileService;
    private final MessageProducer producer;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final SseEmitterService sseEmitterService;

    private static final int DEFAULT_URL_EXPIRATION_MINUTES = 30;

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CV_ID = "cvId";
    private final CloudStorageService cloudStorageService;

    @Override
    public List<ResumeView> getListResumeOfUser(User currentUser) {
        var resumes = resumeQueryDSL.getListResumeOfUser(currentUser != null ? currentUser.getEmail() : "");
        if (resumes.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
        }

        return resumes;
    }

    @Override
    public List<ResumeView> getAnalyzedResumesOfUser(User currentUser) {
        return resumeQueryDSL.getAnalyzedResumesOfUser(currentUser != null ? currentUser.getEmail() : "");
    }

    @Override
    public ResumeDetailDTO getResumeDetail(long id, User user) {
        Resume cv = findResumeAndAssertOwner(id, user, "resume.access.forbidden");
        return new ResumeDetailDTO(cv.getId(), cv.getFileName(), cv.getCreatedAt());
    }

    @Override
    @Transactional
    public ResumeView createResume(ResumeUploadDTO resumeUploadDTO, User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }
        if(resumeRepository.countResumesByUser_Id(user.getId()) > 100){
            throw new BadRequestException(MessageUtils.getMessage("resume.limit_exceeded"));
        }
        MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

        log.info("Creating resume for user: {}, file: {}",
                user.getId(), resumeUploadDTO.getFile().getOriginalFilename());

        // initiate resume
        Resume cv = new Resume();
        cv.setUser(user);
        cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
        String key = KeyGeneratorUtil.generateKey();
        cv.setKeyCf(key);

        //extract data
        byte[] data;
        String rawText;
        try {
            data = fileService.toByteArray(resumeUploadDTO.getFile().getInputStream());
            if (data.length == 0) {
                log.warn("Empty file uploaded during resume creation for user: {}", user.getId());
                throw new BadRequestException(MessageUtils.getMessage("resume.text.empty"));
            }

            rawText = fileService.extractTextFromFile(resumeUploadDTO.getFile().getInputStream());
            rawText = fileService.cleanText(rawText);

            if (rawText == null || rawText.isEmpty()) {
                log.warn("Text extraction yielded empty result for user: {}", user.getId());
                throw new BadRequestException(MessageUtils.getMessage("resume.text.empty"));
            }
            log.info("raw text extracted successfully");
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("File processing failed during resume creation for user: {}", user.getId());
            throw new BadRequestException(MessageUtils.getMessage("resume.text.empty"));
        }

        cv.setRawText(rawText);

        // upload to cloud
        try {
            cloudStorageService.uploadFile(data, key, resumeUploadDTO.getFile().getOriginalFilename());
        }catch (Exception e) {
            log.warn("File processing failed during resume creation for user: {}", user.getId());
            throw new RuntimeException(MessageUtils.getMessage("resume.upload.failed"));
        }finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CV_ID);
        }
        // save to db
        resumeRepository.save(cv);
        MDC.put(MDC_CV_ID, String.valueOf(cv.getId()));

        // send SSE event
        try {
            sseEmitterService.sendEvent(user.getId(), "resume-process",
                    SseMessagePayload.builder()
                            .id(cv.getId())
                            .status("uploaded")
                            .message("File uploaded to cloud successfully")
                            .build());
        } catch (Exception e) {
            log.error("Failed to send SSE uploaded event for user: {}, cv: {}", user.getId(), cv.getId(), e);
        }

        // analyze and vectorize resume
        if (resumeUploadDTO.enableAiAnalysis()) {
            producer.processAI(new ResumeParsingMessage(rawText, user.getId(), cv.getId()));
            log.info("Resume created — cv: {}, dispatched cloud upload and AI processing for user: {}",
                    cv.getId(), user.getId());
        } else {
            log.info("Resume created — cv: {}, skipped AI processing (user opted out) for user: {}",
                    cv.getId(), user.getId());
        }
        return new ResumeView(cv.getId(), cv.getFileName(), cv.getCreatedAt(), cv.isAnalyzed());
    }

    @Override
    @Transactional
    public void analyzeResume(long id, User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }
        Resume cv = findResumeAndAssertOwner(id, user, "resume.access.forbidden");

        if (cv.isAnalyzed()) {
            throw new BadRequestException(MessageUtils.getMessage("resume.already_analyzed"));
        }

        String rawText = cv.getRawText();
        if (rawText == null || rawText.isBlank()) {
            throw new BadRequestException(MessageUtils.getMessage("resume.text.empty"));
        }

        // Send SSE "analyzing" event
        sseEmitterService.sendEvent(user.getId(), "resume-process",
                SseMessagePayload.builder()
                        .id(cv.getId())
                        .status("analyzing")
                        .message("AI is analyzing your resume...")
                        .build());

        // Dispatch to RabbitMQ (async)
        producer.processAI(new ResumeParsingMessage(rawText, user.getId(), cv.getId()));
        log.info("Deferred AI analysis triggered for CV: {} by user: {}", id, user.getId());
    }

    @Override
    public void deleteResume(long id, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_CV_ID, String.valueOf(id));

            Resume cv = findResumeAndAssertOwner(id, user, "resume.delete.forbidden");

            cv.markDeleted();
            resumeRepository.save(cv);
            log.info("Resume soft-deleted — cv: {} by user: {}", id, user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Override
    public ResumeUrlDTO getResumeViewUrl(long id, User user) {
        Resume cv = findResumeAndAssertOwner(id, user, "resume.view.forbidden");
        try {
            String url = s3PresignedUrlService.generateViewUrl(cv.getKeyCf(), DEFAULT_URL_EXPIRATION_MINUTES);
            return new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to generate view URL for CV: {}, user: {}", id, user.getId());
            throw new RuntimeException(MessageUtils.getMessage("resume.url.generate_failed"));
        }
    }

    @Override
    public ResumeUrlDTO getResumeDownloadUrl(long id, User user) {
        Resume cv = findResumeAndAssertOwner(id, user, "resume.download.forbidden");

        try {
            String url = s3PresignedUrlService.generateDownloadUrl(cv.getKeyCf(), cv.getFileName(), DEFAULT_URL_EXPIRATION_MINUTES);
            return new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to generate download URL for CV: {}, user: {}", id, user.getId());
            throw new RuntimeException(MessageUtils.getMessage("resume.url.generate_failed"));
        }
    }

    @Override
    public ResumeUrlDTO getResumeViewUrlForHirer(long id) {
        Resume cv = findResumeById(id);

        try {
            String url = s3PresignedUrlService.generateViewUrl(cv.getKeyCf(), DEFAULT_URL_EXPIRATION_MINUTES);
            return new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to generate hirer view URL for CV: {}", id);
            throw new RuntimeException(MessageUtils.getMessage("resume.url.generate_failed"));
        }
    }

    @Override
    public ResumeUrlDTO getResumeDownloadUrlForHirer(long id) {
        Resume cv = findResumeById(id);

        try {
            String url = s3PresignedUrlService.generateDownloadUrl(cv.getKeyCf(), cv.getFileName(), DEFAULT_URL_EXPIRATION_MINUTES);
            return new ResumeUrlDTO(cv.getId(), cv.getFileName(), url, DEFAULT_URL_EXPIRATION_MINUTES);
        } catch (Exception e) {
            log.warn("Failed to generate hirer download URL for CV: {}", id);
            throw new RuntimeException(MessageUtils.getMessage("resume.url.generate_failed"));
        }
    }

    private Resume findResumeAndAssertOwner(long id, User user, String forbiddenMessageKey) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }
        Resume cv = findResumeById(id);
        if (cv.getUser() == null || !user.getEmail().equals(cv.getUser().getEmail())) {
            log.warn("Resume access forbidden — user: {} does not own CV: {}", user.getId(), id);
            throw new ForbiddenException(MessageUtils.getMessage(forbiddenMessageKey));
        }
        return cv;
    }

    private Resume findResumeById(long id) {
        return resumeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found")));
    }
}
