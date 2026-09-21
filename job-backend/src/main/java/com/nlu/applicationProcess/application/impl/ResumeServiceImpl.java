package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.infrastructure.query.ResumeQueryDSL;
import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.req.ResumeDetailDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeUrlDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.domain.exception.UnauthorizedException;
import com.nlu.applicationProcess.domain.event.ResumeAnalysisRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeStatus;
import com.nlu.identity.domain.model.User;
import com.nlu.applicationProcess.application.ResumeService;
import com.nlu.shared.application.FileService;
import com.nlu.shared.application.S3PresignedUrlService;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.domain.model.SseMessagePayload;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    private final ResumeQueryDSL resumeQueryDSL;
    private final FileService fileService;
    private final ApplicationEventPublisher eventPublisher;
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
        return new ResumeDetailDTO(cv.getId(), cv.getFileName(), cv.getCreatedAt(), cv.getStatus());
    }

    @Override
    @Transactional
    public void analyzeResume(long id, User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }
        Resume cv = findResumeAndAssertOwner(id, user, "resume.access.forbidden");

        if (cv.getStatus() == ResumeStatus.READY) {
            throw new BadRequestException(MessageUtils.getMessage("resume.already_analyzed"));
        }
        if (cv.getStatus() == ResumeStatus.ANALYZING) {
            throw new BadRequestException("Resume is currently being analyzed");
        }

        String rawText = cv.getRawText();
        if (rawText == null || rawText.isBlank()) {
            try {
                byte[] fileBytes = cloudStorageService.getObjectBytes(cv.getKeyCf());
                rawText = fileService.extractTextFromFile(new ByteArrayInputStream(fileBytes));
                if (rawText == null || rawText.isBlank()) {
                    rawText = fileService.extractTextFromFileOcr(fileBytes, cv.getFileName());
                }
                rawText = fileService.cleanText(rawText);
            } catch (Exception e) {
                log.warn("Failed to extract text from file or OCR for resume: {}", cv.getId(), e);
            }

            if (rawText == null || rawText.isBlank()) {
                throw new BadRequestException(MessageUtils.getMessage("resume.text.empty"));
            }

            cv.setRawText(rawText);
            try {
                resumeRepository.save(cv);
            } catch (Exception e) {
                log.error("Failed to update rawText for resume: {}", cv.getId(), e);
                throw new RuntimeException("Failed to update rawText for resume", e);
            }
        }

        cv.startAnalysis();
        resumeRepository.save(cv);

        // Send SSE "analyzing" event
        try {
            sseEmitterService.sendEvent(user.getId(), "resume-process",
                    SseMessagePayload.builder()
                            .id(cv.getId())
                            .status("analyzing")
                            .message("AI is analyzing your resume...")
                            .build());
        } catch (Exception e) {
            log.warn("Failed to send SSE analyzing event for CV: {}, user: {}", id, user.getId(), e);
        }

        // Dispatch Spring event
        eventPublisher.publishEvent(new ResumeAnalysisRequestedEvent(new ResumeParsingMessage(rawText, user.getId(), cv.getId())));
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
