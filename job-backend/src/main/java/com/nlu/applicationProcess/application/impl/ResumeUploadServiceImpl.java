package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.api.dto.req.ResumeUploadInitiateRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.api.dto.res.ResumeUploadInitiateResponse;
import com.nlu.applicationProcess.application.ResumeUploadService;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.application.S3PresignedUrlService;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.domain.exception.StorageException;
import com.nlu.shared.domain.exception.UnauthorizedException;
import com.nlu.shared.domain.model.PresignedUploadUrlResponse;
import com.nlu.shared.domain.model.StorageObjectMetadata;
import com.nlu.shared.infrastructure.config.ResumeUploadProperties;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeUploadServiceImpl implements ResumeUploadService {

    private final ResumeUploadSessionRepository sessionRepository;
    private final ResumeRepository resumeRepository;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final CloudStorageService cloudStorageService;
    private final ResumeUploadFinalizer resumeUploadFinalizer;
    private final ResumeUploadProperties properties;

    private static final Map<String, String> ALLOWED_EXTENSIONS_TO_MIME = Map.of(
            ".pdf", "application/pdf",
            ".doc", "application/msword",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @Override
    public ResumeUploadInitiateResponse initiateUpload(ResumeUploadInitiateRequest request, User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        // Validate size
        if (request.size() == null || request.size() <= 0) {
            throw new BadRequestException("File size must be greater than 0");
        }
        if (request.size() > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException("File size exceeds maximum limit of 5 MB");
        }

        // Validate and sanitize filename
        String sanitizedFileName = sanitizeFileName(request.fileName());

        // Validate extension and strict matching with content type
        validateFileTypeAndExtension(sanitizedFileName, request.contentType());

        // Check resume quota
        if (resumeRepository.countResumesByUser_Id(currentUser.getId()) > 100) {
            throw new BadRequestException(MessageUtils.getMessage("resume.limit_exceeded"));
        }

        // Generate identifiers and storage keys (never include original filename in storage keys)
        UUID uploadId = UUID.randomUUID();
        String tempKey = properties.getTempPrefix() + "/resumes/" + currentUser.getId() + "/" + uploadId;
        String permanentKey = properties.getPermanentPrefix() + "/" + currentUser.getId() + "/" + uploadId;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sessionExpiresAt = now.plusMinutes(properties.getSessionExpirationMinutes());

        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(uploadId)
                .userId(currentUser.getId())
                .originalFileName(sanitizedFileName)
                .declaredContentType(request.contentType().trim().toLowerCase())
                .declaredSize(request.size())
                .tempKey(tempKey)
                .permanentKey(permanentKey)
                .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                .resumeId(null)
                .expiresAt(sessionExpiresAt)
                .createdAt(now)
                .updatedAt(now)
                .build();

        sessionRepository.save(session);
        log.info("Created ResumeUploadSession id: {} for user: {}", uploadId, currentUser.getId());

        // Generate presigned PUT URL
        PresignedUploadUrlResponse presignedResponse = s3PresignedUrlService.generateUploadUrl(
                tempKey,
                session.getDeclaredContentType(),
                uploadId,
                properties.getUrlExpirationMinutes()
        );

        return new ResumeUploadInitiateResponse(
                uploadId,
                presignedResponse.url(),
                presignedResponse.method(),
                presignedResponse.requiredHeaders(),
                presignedResponse.expiresAt()
        );
    }

    @Override
    public ResumeView completeUpload(UUID uploadId, User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        ResumeUploadSession session = sessionRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("Upload session not found: " + uploadId));

        // Check ownership
        if (session.getUserId() != currentUser.getId()) {
            log.warn("User {} tried to complete session {} belonging to user {}",
                    currentUser.getId(), uploadId, session.getUserId());
            throw new ForbiddenException(MessageUtils.getMessage("resume.access.forbidden"));
        }

        // Idempotency: if already COMPLETED, return the existing ResumeView
        if (session.getStatus() == ResumeUploadSessionStatus.COMPLETED) {
            log.info("Session {} is already COMPLETED. Returning existing resume id: {}", uploadId, session.getResumeId());
            Resume resume = resumeRepository.findById(session.getResumeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Associated resume not found: " + session.getResumeId()));
            return new ResumeView(resume.getId(), resume.getFileName(), resume.getCreatedAt(), resume.getStatus());
        }

        // Check expiration
        if (session.getStatus() == ResumeUploadSessionStatus.EXPIRED ||
                LocalDateTime.now().isAfter(session.getExpiresAt())) {
            if (session.getStatus() != ResumeUploadSessionStatus.EXPIRED) {
                session.setStatus(ResumeUploadSessionStatus.EXPIRED);
                sessionRepository.save(session);
            }
            throw new BadRequestException("Upload session has expired: " + uploadId);
        }

        // Check rejected status
        if (session.getStatus() == ResumeUploadSessionStatus.REJECTED) {
            throw new BadRequestException("Upload session was rejected: " + uploadId);
        }

        // Storage verification & copy outside DB transaction
        boolean alreadyCopied = false;
        Optional<StorageObjectMetadata> permMetaOpt = cloudStorageService.headObject(session.getPermanentKey());
        if (permMetaOpt.isPresent()) {
            StorageObjectMetadata permMeta = permMetaOpt.get();
            if (permMeta.contentLength() == session.getDeclaredSize() &&
                    isMatchingContentType(permMeta.contentType(), session.getDeclaredContentType())) {
                log.info("Permanent object already exists with valid metadata for uploadId: {}. Skipping copy.", uploadId);
                alreadyCopied = true;
            }
        }

        if (!alreadyCopied) {
            // HEAD temp object
            Optional<StorageObjectMetadata> tempMetaOpt = cloudStorageService.headObject(session.getTempKey());
            if (tempMetaOpt.isEmpty()) {
                log.warn("Temp object not found for upload session: {}, key: {}", uploadId, session.getTempKey());
                markSessionRejected(session);
                throw new BadRequestException("Uploaded file not found in storage");
            }

            StorageObjectMetadata tempMeta = tempMetaOpt.get();

            // Validate temp object metadata
            boolean isValidMetadata = tempMeta.contentLength() > 0 &&
                    tempMeta.contentLength() <= properties.getMaxFileSizeBytes() &&
                    tempMeta.contentLength() == session.getDeclaredSize() &&
                    ALLOWED_CONTENT_TYPES.contains(normalizeContentType(tempMeta.contentType())) &&
                    isMatchingContentType(tempMeta.contentType(), session.getDeclaredContentType());

            if (!isValidMetadata) {
                log.warn("Invalid metadata for temp object in session {}: size={}, declaredSize={}, type={}, declaredType={}",
                        uploadId, tempMeta.contentLength(), session.getDeclaredSize(),
                        tempMeta.contentType(), session.getDeclaredContentType());
                markSessionRejected(session);
                deleteTempSilently(session.getTempKey());
                throw new BadRequestException("Uploaded file metadata does not match declared specifications");
            }

            // Server-side copy from tempKey to permanentKey
            cloudStorageService.copyObject(session.getTempKey(), session.getPermanentKey());

            // HEAD permanent object to verify copy success and metadata
            StorageObjectMetadata verifiedPermMeta = cloudStorageService.headObject(session.getPermanentKey())
                    .orElseThrow(() -> new StorageException("Failed to verify permanent object after copy"));

            if (verifiedPermMeta.contentLength() != session.getDeclaredSize()) {
                log.error("Permanent object size {} does not match declared size {} after copy for session {}",
                        verifiedPermMeta.contentLength(), session.getDeclaredSize(), uploadId);
                throw new StorageException("Permanent object size mismatch after copy");
            }
        }

        // Finalize upload in short DB transaction
        Resume finalizedResume = resumeUploadFinalizer.finalizeUpload(
                uploadId,
                currentUser,
                session.getPermanentKey(),
                session.getOriginalFileName()
        );

        // Delete temp best-effort after transaction commits
        deleteTempSilently(session.getTempKey());

        return new ResumeView(
                finalizedResume.getId(),
                finalizedResume.getFileName(),
                finalizedResume.getCreatedAt(),
                finalizedResume.getStatus()
        );
    }

    private void markSessionRejected(ResumeUploadSession session) {
        try {
            session.setStatus(ResumeUploadSessionStatus.REJECTED);
            sessionRepository.save(session);
        } catch (Exception e) {
            log.error("Failed to mark session {} as REJECTED", session.getId(), e);
        }
    }

    private void deleteTempSilently(String tempKey) {
        try {
            cloudStorageService.deleteObject(tempKey);
            log.info("Temp object deleted: {}", tempKey);
        } catch (Exception e) {
            log.warn("Failed to delete temp object best-effort: {}. Object lifecycle will handle cleanup.", tempKey, e);
        }
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BadRequestException("File name cannot be empty");
        }
        String name = Paths.get(fileName.trim()).getFileName().toString();
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        if (sanitized.isEmpty()) {
            throw new BadRequestException("File name is invalid");
        }
        return sanitized;
    }

    private void validateFileTypeAndExtension(String fileName, String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new BadRequestException("Content-Type is required");
        }

        String normalizedContentType = normalizeContentType(contentType);
        String lowerCaseName = fileName.toLowerCase();

        String matchingExt = null;
        for (Map.Entry<String, String> entry : ALLOWED_EXTENSIONS_TO_MIME.entrySet()) {
            if (lowerCaseName.endsWith(entry.getKey())) {
                matchingExt = entry.getKey();
                break;
            }
        }

        if (matchingExt == null) {
            throw new BadRequestException("File extension is not allowed. Only PDF, DOC, and DOCX are permitted");
        }

        String expectedMime = ALLOWED_EXTENSIONS_TO_MIME.get(matchingExt);
        if (!expectedMime.equalsIgnoreCase(normalizedContentType)) {
            throw new BadRequestException("File extension '" + matchingExt + "' does not match Content-Type '" + contentType + "'");
        }
    }

    private boolean isMatchingContentType(String actual, String declared) {
        if (actual == null || declared == null) {
            return false;
        }
        return normalizeContentType(actual).equalsIgnoreCase(normalizeContentType(declared));
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        // Strip parameters like charset, e.g. "application/pdf; charset=UTF-8"
        int semicolonIdx = contentType.indexOf(';');
        if (semicolonIdx > 0) {
            return contentType.substring(0, semicolonIdx).trim().toLowerCase();
        }
        return contentType.trim().toLowerCase();
    }
}
