package web_application.application;

import com.nlu.applicationProcess.application.impl.ResumeUploadSessionCleaner;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.applicationProcess.infrastructure.job.ResumeUploadCleanupJob;
import com.nlu.shared.application.CloudStorageService;
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
    private ResumeUploadProperties properties;

    @InjectMocks
    private ResumeUploadCleanupJob cleanupJob;

    @BeforeEach
    void setUp() {
        lenient().when(properties.getCleanupBatchSize()).thenReturn(50);
    }

    @Test
    @DisplayName("Cleanup job tìm các session PENDING_UPLOAD đã hết hạn, đánh dấu EXPIRED và xóa temp best-effort")
    void cleanupExpiredSessions_Success() {
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

        cleanupJob.cleanupExpiredSessions();

        verify(sessionCleaner).markExpired(id1);
        verify(cloudStorageService).deleteObject(session1.getTempKey());
    }

    @Test
    @DisplayName("Cleanup job không đụng tới session COMPLETED và lỗi xóa temp không làm gián đoạn")
    void cleanupExpiredSessions_StorageFailureHandled() {
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
        doThrow(new RuntimeException("R2 delete error")).when(cloudStorageService).deleteObject(session1.getTempKey());

        // Should not throw exception
        cleanupJob.cleanupExpiredSessions();

        verify(sessionCleaner).markExpired(id1);
        verify(cloudStorageService).deleteObject(session1.getTempKey());
    }
}
