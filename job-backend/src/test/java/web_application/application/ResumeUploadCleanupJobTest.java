package web_application.application;

import com.nlu.applicationProcess.application.impl.ResumeUploadFinalizer;
import com.nlu.applicationProcess.application.impl.ResumeUploadSessionCleaner;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.applicationProcess.infrastructure.job.ResumeUploadCleanupJob;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.domain.model.StorageObjectMetadata;
import com.nlu.shared.infrastructure.config.ResumeUploadProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeUploadCleanupJobTest {

    @Mock
    private ResumeUploadSessionRepository sessionRepository;

    @Mock
    private CloudStorageService cloudStorageService;

    @Mock
    private ResumeUploadSessionCleaner sessionCleaner;

    @Mock
    private ResumeUploadFinalizer finalizer;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private ResumeUploadProperties properties;

    @InjectMocks
    private ResumeUploadCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getCleanupBatchSize()).thenReturn(50);
    }

    @Test
    @DisplayName("Cleanup xóa DB row của abandoned session sau khi storage cleanup thành công")
    void cleanupAbandonedSessions_Success_DeletesDbRow() {
        UUID id1 = UUID.randomUUID();
        ResumeUploadSession session1 = ResumeUploadSession.builder()
                .id(id1)
                .tempKey("temp/resumes/1/" + id1)
                .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(sessionRepository.findByStatusAndExpiresAtBefore(
                eq(ResumeUploadSessionStatus.PENDING_UPLOAD),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(session1));

        when(sessionCleaner.markExpired(id1)).thenReturn(true);

        cleanupJob.cleanupAbandonedSessions();

        verify(sessionCleaner).markExpired(id1);
        verify(cloudStorageService).deleteObject(session1.getTempKey());
        // Must delete DB row since storage delete succeeded
        verify(sessionCleaner).deleteExpiredSession(id1);
        verify(sessionCleaner, never()).recordCleanupFailure(eq(id1), anyString());
    }

    @Test
    @DisplayName("Cleanup giữ row khi xóa storage thất bại để cron sau thử lại")
    void cleanupAbandonedSessions_StorageFailure_PreservesDbRow() {
        UUID id1 = UUID.randomUUID();
        ResumeUploadSession session1 = ResumeUploadSession.builder()
                .id(id1)
                .tempKey("temp/resumes/1/" + id1)
                .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        when(sessionRepository.findByStatusAndExpiresAtBefore(
                eq(ResumeUploadSessionStatus.PENDING_UPLOAD),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(session1));

        when(sessionCleaner.markExpired(id1)).thenReturn(true);
        doThrow(new RuntimeException("R2 network glitch")).when(cloudStorageService).deleteObject(session1.getTempKey());

        cleanupJob.cleanupAbandonedSessions();

        verify(sessionCleaner).markExpired(id1);
        verify(cloudStorageService).deleteObject(session1.getTempKey());
        // Must NOT delete DB row, must record failure attempt
        verify(sessionCleaner, never()).deleteExpiredSession(id1);
        verify(sessionCleaner).recordCleanupFailure(eq(id1), contains("glitch"));
    }

    @Test
    @DisplayName("Cleanup xóa object nhưng giữ DB row của REJECTED phục vụ truy vết")
    void cleanupRejectedSessions_DeletesObjects_PreservesDbRow() {
        UUID id = UUID.randomUUID();
        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(id)
                .tempKey("temp/resumes/1/" + id)
                .permanentKey("resumes/1/" + id)
                .status(ResumeUploadSessionStatus.REJECTED)
                .rejectionCode("METADATA_SIZE_MISMATCH")
                .build();

        when(sessionRepository.findByStatusAndCleanupCompletedAtIsNull(
                eq(ResumeUploadSessionStatus.REJECTED),
                any(Pageable.class)
        )).thenReturn(List.of(session));

        cleanupJob.cleanupRejectedSessions();

        verify(cloudStorageService).deleteObject(session.getTempKey());
        verify(cloudStorageService).deleteObject(session.getPermanentKey());
        verify(sessionCleaner).recordCleanupSuccess(id);
        verify(sessionCleaner, never()).deleteExpiredSession(id);
    }

    @Test
    @DisplayName("Cleanup REJECTED khi storage delete thất bại: KHÔNG gọi recordCleanupSuccess, gọi recordCleanupFailure, giữ DB row")
    void cleanupRejectedSessions_StorageFailure_DoesNotRecordSuccess_RecordsFailure() {
        UUID id = UUID.randomUUID();
        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(id)
                .tempKey("temp/resumes/1/" + id)
                .permanentKey("resumes/1/" + id)
                .status(ResumeUploadSessionStatus.REJECTED)
                .rejectionCode("METADATA_SIZE_MISMATCH")
                .build();

        when(sessionRepository.findByStatusAndCleanupCompletedAtIsNull(
                eq(ResumeUploadSessionStatus.REJECTED),
                any(Pageable.class)
        )).thenReturn(List.of(session));

        doThrow(new RuntimeException("S3 500 Internal Error")).when(cloudStorageService).deleteObject(session.getTempKey());
        StorageObjectMetadata existingMeta = new StorageObjectMetadata(session.getTempKey(), 1024L, "application/pdf", "etag");
        when(cloudStorageService.headObject(session.getTempKey())).thenReturn(Optional.of(existingMeta));

        cleanupJob.cleanupRejectedSessions();

        // Must NOT record success
        verify(sessionCleaner, never()).recordCleanupSuccess(id);
        // Must record failure for retry
        verify(sessionCleaner).recordCleanupFailure(eq(id), contains("incomplete"));
        // Must NOT delete REJECTED DB row
        verify(sessionCleaner, never()).deleteExpiredSession(id);
    }

    @Test
    @DisplayName("Cleanup REJECTED bỏ qua xóa permanent key nếu session có resumeId hoặc key đang được Resume tham chiếu")
    void cleanupRejectedSessions_WhenKeyReferencedByResume_SkipsPermanentDeletion() {
        UUID id = UUID.randomUUID();
        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(id)
                .tempKey("temp/resumes/1/" + id)
                .permanentKey("resumes/1/" + id)
                .status(ResumeUploadSessionStatus.REJECTED)
                .rejectionCode("METADATA_SIZE_MISMATCH")
                .build();

        when(sessionRepository.findByStatusAndCleanupCompletedAtIsNull(
                eq(ResumeUploadSessionStatus.REJECTED),
                any(Pageable.class)
        )).thenReturn(List.of(session));

        when(resumeRepository.existsByKeyCf(session.getPermanentKey())).thenReturn(true);

        cleanupJob.cleanupRejectedSessions();

        verify(cloudStorageService).deleteObject(session.getTempKey());
        // Permanent key must NOT be deleted because it is referenced by a Resume
        verify(cloudStorageService, never()).deleteObject(session.getPermanentKey());
        // Both objects considered cleaned (temp deleted, perm skipped because referenced)
        verify(sessionCleaner).recordCleanupSuccess(id);
    }

    @Test
    @DisplayName("Stale FINALIZING được recovery: nếu permanent object hợp lệ thì hoàn tất tạo Resume")
    void recoverStuckFinalizingSessions_WhenPermanentObjectValid_RecoversToCompleted() {
        UUID id = UUID.randomUUID();
        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(id)
                .userId(100L)
                .declaredContentType("application/pdf")
                .declaredSize(2048L)
                .tempKey("temp/resumes/100/" + id)
                .permanentKey("resumes/100/" + id)
                .status(ResumeUploadSessionStatus.FINALIZING)
                .finalizingStartedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(sessionRepository.findByStatusAndFinalizingStartedAtBefore(
                eq(ResumeUploadSessionStatus.FINALIZING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(session));

        StorageObjectMetadata permMeta = new StorageObjectMetadata(
                session.getPermanentKey(),
                2048L,
                "application/pdf",
                "etag123"
        );
        when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.of(permMeta));

        User user = new User();
        user.setId(100L);
        when(userRepository.findById(100L)).thenReturn(Optional.of(user));
        when(finalizer.recoverStuckSession(id, true, user)).thenReturn(true);

        cleanupJob.recoverStuckFinalizingSessions();

        verify(finalizer).recoverStuckSession(id, true, user);
    }

    @Test
    @DisplayName("Stale FINALIZING recovery từ chối khi permanent object sai content type (chuyển sang REJECTED)")
    void recoverStuckFinalizingSessions_WhenContentTypeMismatch_RecoversToRejected() {
        UUID id = UUID.randomUUID();
        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(id)
                .userId(100L)
                .declaredContentType("application/pdf")
                .declaredSize(2048L)
                .tempKey("temp/resumes/100/" + id)
                .permanentKey("resumes/100/" + id)
                .status(ResumeUploadSessionStatus.FINALIZING)
                .finalizingStartedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(sessionRepository.findByStatusAndFinalizingStartedAtBefore(
                eq(ResumeUploadSessionStatus.FINALIZING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(session));

        // Object has mismatched content-type "image/png"
        StorageObjectMetadata permMeta = new StorageObjectMetadata(
                session.getPermanentKey(),
                2048L,
                "image/png",
                "etag123"
        );
        when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.of(permMeta));
        when(finalizer.recoverStuckSession(id, false, null)).thenReturn(true);

        cleanupJob.recoverStuckFinalizingSessions();

        // Must recover to REJECTED (permanentObjectValid = false)
        verify(finalizer).recoverStuckSession(id, false, null);
        verify(cloudStorageService).deleteObject(session.getTempKey());
        verify(cloudStorageService).deleteObject(session.getPermanentKey());
    }

    @Test
    @DisplayName("Stale FINALIZING được recovery: nếu permanent object không tồn tại thì chuyển REJECTED và dọn dẹp orphan")
    void recoverStuckFinalizingSessions_WhenPermanentObjectInvalid_RecoversToRejected() {
        UUID id = UUID.randomUUID();
        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(id)
                .userId(100L)
                .declaredContentType("application/pdf")
                .declaredSize(2048L)
                .tempKey("temp/resumes/100/" + id)
                .permanentKey("resumes/100/" + id)
                .status(ResumeUploadSessionStatus.FINALIZING)
                .finalizingStartedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(sessionRepository.findByStatusAndFinalizingStartedAtBefore(
                eq(ResumeUploadSessionStatus.FINALIZING),
                any(LocalDateTime.class),
                any(Pageable.class)
        )).thenReturn(List.of(session));

        when(cloudStorageService.headObject(session.getPermanentKey())).thenReturn(Optional.empty());
        when(finalizer.recoverStuckSession(id, false, null)).thenReturn(true);

        cleanupJob.recoverStuckFinalizingSessions();

        verify(finalizer).recoverStuckSession(id, false, null);
        verify(cloudStorageService).deleteObject(session.getTempKey());
        verify(cloudStorageService).deleteObject(session.getPermanentKey());
    }
}
