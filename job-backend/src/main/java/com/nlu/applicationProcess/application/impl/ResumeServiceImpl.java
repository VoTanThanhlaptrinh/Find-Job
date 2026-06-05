package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.infrastructure.query.ResumeQueryDSL;
import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.req.ResumeDetailDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeUploadDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeUrlDTO;
import com.nlu.shared.api.message.dto.CloudUploadMessage;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
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
import com.nlu.shared.utils.KeyGeneratorUtil;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeServiceImpl implements ResumeService {
    private final ResumeRepository resumeRepository;
    private final ResumeQueryDSL resumeQueryDSL;
    private final FileService fileService;
    private final MessageProducer producer;
    private final ResumeParsingService resumeParsingService;
    private final S3PresignedUrlService s3PresignedUrlService;

    private static final int DEFAULT_URL_EXPIRATION_MINUTES = 30;

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_CV_ID = "cvId";

    @Override
    public List<ResumeView> getListResumeOfUser(User currentUser) {
        var resumes = resumeQueryDSL.getListResumeOfUser(currentUser != null ? currentUser.getEmail() : "");
        if (resumes.isEmpty()) {
            throw new ResourceNotFoundException(MessageUtils.getMessage("resume.not_found"));
        }

        return resumes;
    }

    @Override
    public ResumeDetailDTO getResumeDetail(long id, User user) {
        Resume cv = findResumeAndAssertOwner(id, user, "resume.access.forbidden");
        return new ResumeDetailDTO(cv.getId(), cv.getFileName(), cv.getCreateDate());
    }

    @Override
    public ResumeView createResume(ResumeUploadDTO resumeUploadDTO, User user) {
        if (user == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            log.info("Creating resume for user: {}, file: {}",
                    user.getId(), resumeUploadDTO.getFile().getOriginalFilename());

            Resume cv = new Resume();
            cv.setUser(user);
            cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
            String key = KeyGeneratorUtil.generateKey();
            cv.setKeyCf(key);
            byte[] data;
            String rawText;
            try {
                data = fileService.toByteArray(resumeUploadDTO.getFile().getInputStream());
                rawText = fileService.extractTextFromFile(resumeUploadDTO.getFile().getInputStream());
                if (rawText == null || rawText.isEmpty()) {
                    log.info("Standard text extraction yielded empty result for user: {} — falling back to OCR", user.getId());
                    rawText = fileService.extractTextFromFileOcr(resumeUploadDTO.getFile());
                }
                rawText = fileService.cleanText(rawText);
            } catch (Exception e) {
                log.warn("File processing failed during resume creation for user: {}", user.getId());
                throw new RuntimeException(MessageUtils.getMessage("resume.upload.failed"));
            }
            if (data.length == 0) {
                log.warn("Empty file uploaded during resume creation for user: {}", user.getId());
                throw new RuntimeException(MessageUtils.getMessage("message.error"));
            }

            producer.uploadToCloud(new CloudUploadMessage(data, key, resumeUploadDTO.getFile().getOriginalFilename()));
            resumeRepository.save(cv);
            MDC.put(MDC_CV_ID, String.valueOf(cv.getId()));

            producer.processAI(new ResumeParsingMessage(rawText, user.getId(), cv.getId()));

            log.info("Resume created — cv: {}, dispatched cloud upload and AI processing for user: {}",
                    cv.getId(), user.getId());

            return new ResumeView(cv.getId(), cv.getFileName(), cv.getCreateDate());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CV_ID);
        }
    }

    @Override
    public void updateResume(long id, ResumeUploadDTO resumeUploadDTO, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_CV_ID, String.valueOf(id));

            log.info("Updating resume: {} for user: {}", id, user.getId());

            Resume cv = findResumeAndAssertOwner(id, user, "resume.edit.forbidden");

            cv.setFileName(resumeUploadDTO.getFile().getOriginalFilename());
            String key = cv.getKeyCf();
            byte[] data;
            try {
                data = fileService.toByteArray(resumeUploadDTO.getFile().getInputStream());
            } catch (Exception e) {
                log.warn("File processing failed during resume update for CV: {}", id);
                throw new RuntimeException(MessageUtils.getMessage("resume.upload.failed"));
            }
            if (data.length == 0) {
                log.warn("Empty file uploaded during resume update for CV: {}", id);
                throw new RuntimeException(MessageUtils.getMessage("message.error"));
            }

            producer.uploadToCloud(new CloudUploadMessage(data, key, resumeUploadDTO.getFile().getOriginalFilename()));
            resumeRepository.save(cv);

            log.info("Resume updated — cv: {}, dispatched cloud upload for user: {}", id, user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_CV_ID);
        }
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
