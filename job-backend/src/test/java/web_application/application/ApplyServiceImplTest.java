package web_application.application;

import com.job_web.data.ApplyRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.ResumeRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.application.ApplyCvWithExistingRequest;
import com.job_web.dto.application.ApplyCvWithUploadRequest;
import com.job_web.dto.common.ApiResponse;
import com.job_web.message.MessageProducer;
import com.job_web.models.Apply;
import com.job_web.models.Job;
import com.job_web.models.Resume;
import com.job_web.models.User;
import com.job_web.service.application.ResumeService;
import com.job_web.service.application.impl.ApplyServiceImpl;
import com.job_web.service.support.FileService;
import com.job_web.utills.MessageUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplyServiceImplTest {

    @Mock
    private ApplyRepository applyRepository;

    @Mock
    private ResumeRepository resumeRepository;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ResumeService resumeService;

    @Mock
    private MessageProducer messageProducer;

    @Mock
    private FileService fileService;

    @InjectMocks
    private ApplyServiceImpl applyService;

    @BeforeEach
    void setUp() {
        MessageSource messageSource = org.mockito.Mockito.mock(MessageSource.class);
        lenient().when(messageSource.getMessage(anyString(), any(), anyString(), any()))
                .thenAnswer(invocation -> invocation.getArgument(2));
        new MessageUtils(messageSource);
    }

    @Nested
    @DisplayName("applyWithUploadCv Tests")
    class ApplyWithUploadCvTests {

        @Test
        @DisplayName("AU01: Van apply thanh cong voi detached user")
        void applyWithUploadCv_Success_WithDetachedUser() throws IOException {
            User detachedUser = new User();
            detachedUser.setId(7L);
            detachedUser.setEmail("user@test.com");

            User managedUser = new User();
            managedUser.setId(7L);
            managedUser.setEmail("user@test.com");

            Job job = new Job();
            job.setId(800L);

            MockMultipartFile file = new MockMultipartFile(
                    "cvFile",
                    "resume.pdf",
                    "application/pdf",
                    "test resume".getBytes()
            );
            ApplyCvWithUploadRequest request = new ApplyCvWithUploadRequest(800L, file, null);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(managedUser));
            when(applyRepository.findByJobAndUser("user@test.com", 800L)).thenReturn(Optional.empty());
            when(jobRepository.findById(800L)).thenReturn(Optional.of(job));
            when(resumeRepository.countActiveByUserEmail("user@test.com")).thenReturn(0L);
            when(resumeService.toByteArray(any())).thenReturn("bytes".getBytes());
            when(fileService.extractTextFromFile(any())).thenReturn("raw text");
            when(resumeRepository.save(any(Resume.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(applyRepository.save(any(Apply.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ApiResponse<String> response = applyService.applyWithUploadCv(request, detachedUser);

            ArgumentCaptor<Apply> applyCaptor = ArgumentCaptor.forClass(Apply.class);
            verify(applyRepository).save(applyCaptor.capture());

            assertEquals(200, response.getStatus());
            assertSame(managedUser, applyCaptor.getValue().getUser());
            assertEquals(800L, applyCaptor.getValue().getJob().getId());
        }

        @Test
        @DisplayName("AU02: Chan upload khi da co 5 CV")
        void applyWithUploadCv_MaxResumeCount() throws IOException {
            User detachedUser = new User();
            detachedUser.setEmail("user@test.com");

            User managedUser = new User();
            managedUser.setEmail("user@test.com");

            MockMultipartFile file = new MockMultipartFile(
                    "cvFile",
                    "resume.pdf",
                    "application/pdf",
                    "test resume".getBytes()
            );
            ApplyCvWithUploadRequest request = new ApplyCvWithUploadRequest(800L, file, null);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(managedUser));
            when(applyRepository.findByJobAndUser("user@test.com", 800L)).thenReturn(Optional.empty());
            when(jobRepository.findById(800L)).thenReturn(Optional.of(new Job()));
            when(resumeRepository.countActiveByUserEmail("user@test.com")).thenReturn(5L);

            ApiResponse<String> response = applyService.applyWithUploadCv(request, detachedUser);

            assertEquals(400, response.getStatus());
        }
    }

    @Nested
    @DisplayName("applyWithExistingCv Tests")
    class ApplyWithExistingCvTests {

        @Test
        @DisplayName("AE01: Van apply thanh cong voi detached user")
        void applyWithExistingCv_Success_WithDetachedUser() {
            User detachedUser = new User();
            detachedUser.setId(7L);
            detachedUser.setEmail("user@test.com");

            User managedUser = new User();
            managedUser.setId(7L);
            managedUser.setEmail("user@test.com");

            Job job = new Job();
            job.setId(123L);

            Resume resume = new Resume();
            resume.setId(456L);

            ApplyCvWithExistingRequest request = new ApplyCvWithExistingRequest(123L, 456L, "user@test.com", null);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(managedUser));
            when(jobRepository.findById(123L)).thenReturn(Optional.of(job));
            when(resumeRepository.findById(456L)).thenReturn(Optional.of(resume));
            when(applyRepository.findByJobAndUser("user@test.com", 123L)).thenReturn(Optional.empty());
            when(resumeRepository.countOwnedByUser(456L, "user@test.com")).thenReturn(1L);
            when(applyRepository.save(any(Apply.class))).thenAnswer(invocation -> invocation.getArgument(0));

            ApiResponse<String> response = applyService.applyWithExistingCv(request, detachedUser);

            ArgumentCaptor<Apply> applyCaptor = ArgumentCaptor.forClass(Apply.class);
            verify(applyRepository).save(applyCaptor.capture());

            assertEquals(200, response.getStatus());
            assertSame(managedUser, applyCaptor.getValue().getUser());
            assertEquals(456L, applyCaptor.getValue().getResume().getId());
        }
    }
}
