package com.nlu.applicationProcess.application.impl;

import com.nlu.applicationProcess.api.dto.req.ResumeUploadInitiateRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.api.dto.res.ResumeUploadInitiateResponse;
import com.nlu.applicationProcess.application.ResumeUploadService;
import com.nlu.applicationProcess.domain.model.ClaimResult;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.applicationProcess.infrastructure.redis.ResumeUploadLockService;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.application.S3PresignedUrlService;
import com.nlu.shared.domain.exception.*;
import com.nlu.shared.domain.model.PresignedUploadUrlResponse;
import com.nlu.shared.domain.model.StorageObjectMetadata;
import com.nlu.shared.infrastructure.config.ResumeUploadProperties;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
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
    private final ResumeUploadLockService lockService;
    private final ResumeUploadProperties properties;

    private static final Map<String, String> ALLOWED_EXTENSIONS_TO_MIME = Map.of(
            ".pdf", "application/pdf",
            ".doc", "application/msword",
            ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    public static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    @Override
    public ResumeUploadInitiateResponse initiateUpload(String idempotencyKey, ResumeUploadInitiateRequest request, User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        // Validate idempotency key
        String trimmedKey = validateAndNormalizeIdempotencyKey(idempotencyKey);

        // Validate size
        if (request.size() == null || request.size() <= 0) {
            throw new BadRequestException("File size must be greater than 0");
        }

        if (request.size() > properties.getMaxFileSizeBytes()) {
            throw new BadRequestException(MessageUtils.getMessage("resume.file_size_exceeded"));
        }

        // Validate and sanitize filename
        String sanitizedFileName = sanitizeFileName(request.fileName());

        // Validate extension and strict matching with content type
        validateFileTypeAndExtension(sanitizedFileName, request.contentType());

        // Check resume quota
        if (resumeRepository.countResumesByUser_Id(currentUser.getId()) >= 100) {
            throw new BadRequestException(MessageUtils.getMessage("resume.limit_exceeded"));
        }

        String normalizedContentType = normalizeContentType(request.contentType());
        String fingerprint = calculateFingerprint(sanitizedFileName, request.size(), normalizedContentType);

        // Try Redis lock for short-term double click prevention
        String lockToken = UUID.randomUUID().toString();
        Optional<String> acquiredLockOpt = lockService.acquireLock(currentUser.getId(), trimmedKey, lockToken);

        if (acquiredLockOpt.isEmpty()) {
            // Lock held by another thread or Redis unavailable. Check DB for existing session.
            Optional<ResumeUploadSession> existingSession = waitForExistingSession(currentUser.getId(), trimmedKey);
            if (existingSession.isPresent()) {
                return handleExistingSession(existingSession.get(), fingerprint);
            }
        }

        try {
            // Check if session already exists in DB
            Optional<ResumeUploadSession> existingSessionOpt = sessionRepository.findByUserIdAndIdempotencyKey(
                    currentUser.getId(), trimmedKey);

            if (existingSessionOpt.isPresent()) {
                return handleExistingSession(existingSessionOpt.get(), fingerprint);
            }

            // Create new upload session
            UUID uploadId = UUID.randomUUID();
            String tempKey = properties.getTempPrefix() + "/resumes/" + currentUser.getId() + "/" + uploadId;
            String permanentKey = properties.getPermanentPrefix() + "/" + currentUser.getId() + "/" + uploadId;

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime sessionExpiresAt = now.plusMinutes(properties.getSessionExpirationMinutes());

            ResumeUploadSession session = ResumeUploadSession.builder()
                    .id(uploadId)
                    .userId(currentUser.getId())
                    .idempotencyKey(trimmedKey)
                    .requestFingerprint(fingerprint)
                    .originalFileName(sanitizedFileName)
                    .declaredContentType(normalizedContentType)
                    .declaredSize(request.size())
                    .tempKey(tempKey)
                    .permanentKey(permanentKey)
                    .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                    .resumeId(null)
                    .cleanupAttempts(0)
                    .expiresAt(sessionExpiresAt)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            try {
                sessionRepository.save(session);
                log.info("Created ResumeUploadSession id: {} for user: {} with idempotencyKey: {}",
                        uploadId, currentUser.getId(), trimmedKey);
            } catch (DataIntegrityViolationException dive) {
                // Concurrency fallback when Redis lock was bypassed or unavailable
                log.info("Concurrent insert race detected for user {} and key {}. Reading existing record.",
                        currentUser.getId(), trimmedKey);
                ResumeUploadSession racedSession = sessionRepository.findByUserIdAndIdempotencyKey(currentUser.getId(), trimmedKey)
                        .orElseThrow(() -> dive);
                return handleExistingSession(racedSession, fingerprint);
            }

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
        } finally {
            if (acquiredLockOpt.isPresent()) {
                lockService.releaseLock(currentUser.getId(), trimmedKey, lockToken);
            }
        }
    }

    private ResumeUploadInitiateResponse handleExistingSession(ResumeUploadSession existingSession, String requestFingerprint) {
        // Enforce fingerprint match
        if (!Objects.equals(existingSession.getRequestFingerprint(), requestFingerprint)) {
            log.warn("Idempotency conflict for user {} and key {}. Stored fingerprint: {}, Request: {}",
                    existingSession.getUserId(), existingSession.getIdempotencyKey(),
                    existingSession.getRequestFingerprint(), requestFingerprint);
            throw new ConflictException("Idempotency key has already been used with different request parameters");
        }

        log.info("Idempotent initiate request for session {}. Current status: {}", existingSession.getId(), existingSession.getStatus());

        switch (existingSession.getStatus()) {
            case PENDING_UPLOAD -> {
                if (LocalDateTime.now().isAfter(existingSession.getExpiresAt())) {
                    throw new BadRequestException("Upload session has expired: " + existingSession.getId());
                }
                // Generate a fresh presigned upload URL for the same tempKey
                PresignedUploadUrlResponse presignedResponse = s3PresignedUrlService.generateUploadUrl(
                        existingSession.getTempKey(),
                        existingSession.getDeclaredContentType(),
                        existingSession.getId(),
                        properties.getUrlExpirationMinutes()
                );
                return new ResumeUploadInitiateResponse(
                        existingSession.getId(),
                        presignedResponse.url(),
                        presignedResponse.method(),
                        presignedResponse.requiredHeaders(),
                        presignedResponse.expiresAt()
                );
            }
            case FINALIZING -> {
                throw new ConflictException("Upload session is currently finalizing. Please check status or retry complete.");
            }
            case COMPLETED -> {
                log.info("Upload session {} already COMPLETED. Returning existing resume id: {}",
                        existingSession.getId(), existingSession.getResumeId());
                return new ResumeUploadInitiateResponse(
                        existingSession.getId(),
                        null,
                        null,
                        Map.of(),
                        existingSession.getExpiresAt()
                );
            }
            case REJECTED -> {
                String code = existingSession.getRejectionCode() != null ? existingSession.getRejectionCode() : "UPLOAD_REJECTED";
                String detail = existingSession.getRejectionDetail() != null ? existingSession.getRejectionDetail() : "Upload session was rejected";
                throw new BadRequestException("Upload session was rejected: " + code + " - " + detail);
            }
            case EXPIRED -> {
                throw new BadRequestException("Upload session has expired: " + existingSession.getId());
            }
            default -> throw new BadRequestException("Unknown session status: " + existingSession.getStatus());
        }
    }

    private Optional<ResumeUploadSession> waitForExistingSession(long userId, String idempotencyKey) {
        for (int i = 0; i < 10; i++) {
            Optional<ResumeUploadSession> session = sessionRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (session.isPresent()) {
                return session;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return Optional.empty();
    }

    @Override
    public ResumeView completeUpload(UUID uploadId, User currentUser) {
        if (currentUser == null) {
            throw new UnauthorizedException(MessageUtils.getMessage("message.unauthorized"));
        }

        // Claim session in short database transaction: PENDING_UPLOAD -> FINALIZING
        ClaimResult claimResult = resumeUploadFinalizer.claimForFinalizing(uploadId, currentUser);

        if (claimResult.status() == ClaimResult.ClaimStatus.ALREADY_COMPLETED) {
            log.info("Session {} is already COMPLETED. Returning existing resume id: {}",
                    uploadId, claimResult.existingResumeId());
            Resume resume = resumeRepository.findById(claimResult.existingResumeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Associated resume not found: " + claimResult.existingResumeId()));
            return new ResumeView(resume.getId(), resume.getFileName(), resume.getCreatedAt(), resume.getStatus());
        }

        if (claimResult.status() == ClaimResult.ClaimStatus.IN_PROGRESS) {
            log.info("Session {} is currently FINALIZING. Waiting for completion...", uploadId);
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                Optional<ResumeUploadSession> pollOpt = sessionRepository.findById(uploadId);
                if (pollOpt.isPresent() && pollOpt.get().getStatus() == ResumeUploadSessionStatus.COMPLETED) {
                    Resume resume = resumeRepository.findById(pollOpt.get().getResumeId())
                            .orElseThrow(() -> new ResourceNotFoundException("Associated resume not found: " + pollOpt.get().getResumeId()));
                    return new ResumeView(resume.getId(), resume.getFileName(), resume.getCreatedAt(), resume.getStatus());
                }
            }
            throw new ConflictException("Upload finalization is in progress. Please retry shortly.");
        }

        String processingToken = claimResult.processingToken();
        ResumeUploadSession session = claimResult.session();

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
                resumeUploadFinalizer.markRejected(uploadId, processingToken, "TEMP_OBJECT_NOT_FOUND",
                        "Uploaded file not found in storage", null, null, null);
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
                String code = (tempMeta.contentLength() != session.getDeclaredSize())
                        ? "METADATA_SIZE_MISMATCH"
                        : "METADATA_CONTENT_TYPE_MISMATCH";
                resumeUploadFinalizer.markRejected(uploadId, processingToken, code,
                        "Uploaded file metadata does not match declared specifications",
                        tempMeta.contentLength(), tempMeta.contentType(), tempMeta.etag());
                deleteTempSilently(session.getTempKey());
                throw new BadRequestException("Uploaded file metadata does not match declared specifications");
            }

            // Server-side copy from tempKey to permanentKey
            cloudStorageService.copyObject(session.getTempKey(), session.getPermanentKey());

            // HEAD permanent object to verify copy success and metadata
            Optional<StorageObjectMetadata> verifiedPermMetaOpt = cloudStorageService.headObject(session.getPermanentKey());
            boolean permValid = verifiedPermMetaOpt.isPresent() &&
                    verifiedPermMetaOpt.get().contentLength() > 0 &&
                    verifiedPermMetaOpt.get().contentLength() == session.getDeclaredSize() &&
                    ALLOWED_CONTENT_TYPES.contains(normalizeContentType(verifiedPermMetaOpt.get().contentType())) &&
                    isMatchingContentType(verifiedPermMetaOpt.get().contentType(), session.getDeclaredContentType());

            if (!permValid) {
                log.error("Permanent object verification failed after copy for session {}", uploadId);
                deleteObjectSilently(session.getPermanentKey());
                Long actualSize = verifiedPermMetaOpt.map(StorageObjectMetadata::contentLength).orElse(null);
                String actualType = verifiedPermMetaOpt.map(StorageObjectMetadata::contentType).orElse(null);
                String actualEtag = verifiedPermMetaOpt.map(StorageObjectMetadata::etag).orElse(null);
                resumeUploadFinalizer.markRejected(uploadId, processingToken, "PERMANENT_VERIFICATION_FAILED",
                        "Permanent object verification failed after copy", actualSize, actualType, actualEtag);
                throw new StorageException("Permanent object verification failed after copy");
            }
        }

        // Finalize upload in short DB transaction
        Resume finalizedResume = resumeUploadFinalizer.finalizeUpload(
                uploadId,
                processingToken,
                currentUser
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

    private void deleteTempSilently(String tempKey) {
        deleteObjectSilently(tempKey);
    }

    private void deleteObjectSilently(String key) {
        try {
            cloudStorageService.deleteObject(key);
            log.info("Object deleted: {}", key);
        } catch (Exception e) {
            log.warn("Failed to delete object best-effort: {}. Error: {}", key, e.getMessage());
        }
    }

    private String validateAndNormalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }
        String trimmed = idempotencyKey.trim();
        if (trimmed.length() > 128) {
            throw new BadRequestException("Idempotency-Key must not exceed 128 characters");
        }
        try {
            UUID.fromString(trimmed);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Idempotency-Key must be a valid UUID");
        }
        return trimmed;
    }

    private String calculateFingerprint(String fileName, Long size, String contentType) {
        String payload = fileName.trim().toLowerCase() + "|" + size + "|" + contentType.trim().toLowerCase();
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return payload;
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

    public static boolean isMatchingContentType(String actual, String declared) {
        if (actual == null || declared == null) {
            return false;
        }
        return normalizeContentType(actual).equalsIgnoreCase(normalizeContentType(declared));
    }

    public static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        int semicolonIdx = contentType.indexOf(';');
        if (semicolonIdx > 0) {
            return contentType.substring(0, semicolonIdx).trim().toLowerCase();
        }
        return contentType.trim().toLowerCase();
    }
}
