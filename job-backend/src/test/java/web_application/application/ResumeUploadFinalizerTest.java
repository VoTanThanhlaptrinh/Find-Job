package web_application.application;

import com.nlu.applicationProcess.application.impl.ResumeUploadFinalizer;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeStatus;
import com.nlu.applicationProcess.domain.model.ResumeUploadSession;
import com.nlu.applicationProcess.domain.model.ResumeUploadSessionStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.domain.repository.ResumeUploadSessionRepository;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.domain.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeUploadFinalizerTest {

    @Mock
    private ResumeUploadSessionRepository sessionRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private ResumeUploadFinalizer resumeUploadFinalizer;

    private User user;
    private UUID uploadId;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setEmail(new com.nlu.identity.domain.vo.EmailAddress("user42@test.com"));
        uploadId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Finalizer tạo đúng một Resume với status UPLOADED và rawText null, cập nhật session COMPLETED")
    void finalizeUpload_Success() {
        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(uploadId)
                .userId(42L)
                .originalFileName("my_resume.pdf")
                .declaredContentType("application/pdf")
                .declaredSize(1024L)
                .tempKey("temp/resumes/42/" + uploadId)
                .permanentKey("resumes/42/" + uploadId)
                .status(ResumeUploadSessionStatus.PENDING_UPLOAD)
                .build();

        when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> {
            Resume r = invocation.getArgument(0);
            r.setId(1001L);
            return r;
        });

        Resume result = resumeUploadFinalizer.finalizeUpload(uploadId, user, session.getPermanentKey(), session.getOriginalFileName());

        assertNotNull(result);
        assertEquals(1001L, result.getId());
        assertEquals(ResumeStatus.UPLOADED, result.getStatus());
        assertNull(result.getRawText(), "rawText must be null");
        assertEquals("resumes/42/" + uploadId, result.getKeyCf());
        assertEquals("my_resume.pdf", result.getFileName());

        // Session must be updated to COMPLETED with resumeId
        assertEquals(ResumeUploadSessionStatus.COMPLETED, session.getStatus());
        assertEquals(1001L, session.getResumeId());
        verify(sessionRepository).save(session);
    }

    @Test
    @DisplayName("Finalizer idempotent: Khi session đã COMPLETED dưới lock, trả về Resume hiện tại, không tạo Resume mới")
    void finalizeUpload_Idempotent_WhenAlreadyCompleted() {
        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(uploadId)
                .userId(42L)
                .originalFileName("my_resume.pdf")
                .status(ResumeUploadSessionStatus.COMPLETED)
                .resumeId(1001L)
                .build();

        Resume existingResume = new Resume();
        existingResume.setId(1001L);
        existingResume.markUploaded();

        when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));
        when(resumeRepository.findById(1001L)).thenReturn(Optional.of(existingResume));

        Resume result = resumeUploadFinalizer.finalizeUpload(uploadId, user, "resumes/42/" + uploadId, "my_resume.pdf");

        assertEquals(1001L, result.getId());
        // Verify save was never called on resumeRepository
        verify(resumeRepository, never()).save(any(Resume.class));
    }

    @Test
    @DisplayName("Finalizer từ chối session EXPIRED hoặc REJECTED")
    void finalizeUpload_RejectsInvalidSessionStatus() {
        ResumeUploadSession session = ResumeUploadSession.builder()
                .id(uploadId)
                .status(ResumeUploadSessionStatus.EXPIRED)
                .build();

        when(sessionRepository.findByIdForUpdate(uploadId)).thenReturn(Optional.of(session));

        assertThrows(BadRequestException.class, () ->
                resumeUploadFinalizer.finalizeUpload(uploadId, user, "key", "file.pdf")
        );
    }
}
