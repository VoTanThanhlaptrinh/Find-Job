package web_application.messaging;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.req.ApplyCvWithUploadRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeUploadDTO;
import com.nlu.applicationProcess.application.impl.JobApplicationServiceImpl;
import com.nlu.applicationProcess.application.impl.ResumeServiceImpl;
import com.nlu.applicationProcess.domain.event.ResumeAnalysisRequestedEvent;
import com.nlu.applicationProcess.domain.model.JobApplication;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.repository.JobApplicationRepository;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.recruitment.api.dto.JobDto;
import com.nlu.recruitment.application.impl.JobServiceImpl;
import com.nlu.recruitment.domain.event.JobAnalysisRequestedEvent;
import com.nlu.recruitment.domain.model.Address;
import com.nlu.recruitment.domain.model.Category;
import com.nlu.recruitment.domain.model.Job;
import com.nlu.recruitment.domain.model.Recruitment;
import com.nlu.recruitment.domain.repository.AddressRepository;
import com.nlu.recruitment.domain.repository.CategoryRepository;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.recruitment.domain.repository.RecruitmentRepository;
import com.nlu.recruitment.domain.vo.EmploymentType;
import com.nlu.recruitment.mapper.JobMapper;
import com.nlu.recruitment.mapper.JobViewMapper;
import com.nlu.shared.application.CloudStorageService;
import org.springframework.boot.actuate.metrics.data.DefaultRepositoryTagsProvider;
import com.nlu.shared.application.FileService;
import com.nlu.shared.application.HtmlParserService;
import com.nlu.shared.application.S3PresignedUrlService;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.infrastructure.message.MessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApplicationServiceEventPublishingTest {

    // Common mocks
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private SseEmitterService sseEmitterService;

    // JobServiceImpl mocks
    @Mock
    private JobRepository jobRepository;
    @Mock
    private RecruitmentRepository recruitmentRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private DefaultRepositoryTagsProvider repositoryTagsProvider;
    @Mock
    private JobMapper jobMapper;
    @Mock
    private JobViewMapper jobViewMapper;
    @Mock
    private HtmlParserService htmlParserService;

    @InjectMocks
    private JobServiceImpl jobService;

    // ResumeServiceImpl mocks
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private FileService fileService;
    @Mock
    private CloudStorageService cloudStorageService;
    @Mock
    private S3PresignedUrlService s3PresignedUrlService;

    @InjectMocks
    private ResumeServiceImpl resumeService;

    // JobApplicationServiceImpl mocks
    @Mock
    private JobApplicationRepository jobApplicationRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JobApplicationServiceImpl jobApplicationService;

    @Test
    @DisplayName("Verify services do NOT contain MessageProducer field (decoupled)")
    void testServicesDoNotDependOnMessageProducer() {
        assertNoMessageProducerField(JobServiceImpl.class);
        assertNoMessageProducerField(ResumeServiceImpl.class);
        assertNoMessageProducerField(JobApplicationServiceImpl.class);
    }

    private void assertNoMessageProducerField(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        boolean hasProducer = Arrays.stream(fields)
                .anyMatch(f -> f.getType().equals(MessageProducer.class));
        assertFalse(hasProducer, clazz.getSimpleName() + " must NOT depend directly on MessageProducer");
    }

    @Test
    @DisplayName("JobServiceImpl.createJob publishes JobAnalysisRequestedEvent when AI is enabled")
    void testJobServiceCreateJobPublishesEvent() {
        User user = new User();
        user.setId(10L);

        Recruitment recruitment = mock(Recruitment.class);
        when(recruitment.getId()).thenReturn(20L);
        Address address = mock(Address.class);
        when(address.getId()).thenReturn(30L);
        when(recruitment.isExistAddress(address)).thenReturn(true);

        Category category = new Category();
        category.setId(40L);

        Job job = new Job();
        job.setId(50L);
        job.setTitle("Software Engineer");

        JobDto jobDto = new JobDto(
                "Software Engineer",
                30L,
                EmploymentType.FULL_TIME,
                "2000$",
                "Job Desc",
                "Job Req",
                "Java, Spring",
                LocalDate.now().plusDays(10),
                "More detail",
                2,
                true, // enableAiAnalysis = true
                40L
        );

        when(recruitmentRepository.findRecruitmentByUser(user)).thenReturn(Optional.of(recruitment));
        when(addressRepository.findById(30L)).thenReturn(Optional.of(address));
        when(categoryRepository.findById(40L)).thenReturn(Optional.of(category));
        when(jobMapper.toJob(jobDto)).thenReturn(job);
        when(jobRepository.save(job)).thenReturn(job);
        when(htmlParserService.parseHtml(any())).thenReturn("parsed text");

        jobService.createJob(jobDto, user);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertTrue(eventCaptor.getValue() instanceof JobAnalysisRequestedEvent);
        JobAnalysisRequestedEvent event = (JobAnalysisRequestedEvent) eventCaptor.getValue();
        assertNotNull(event.request());
        assertEquals(50L, event.request().getJobId());
        assertEquals(10L, event.request().getUserId());
    }

    @Test
    @DisplayName("JobServiceImpl.analyzeJob publishes JobAnalysisRequestedEvent")
    void testJobServiceAnalyzeJobPublishesEvent() {
        User user = new User();
        user.setId(10L);

        Recruitment recruitment = mock(Recruitment.class);
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(50L);
        when(job.isOwnedBy(recruitment)).thenReturn(true);
        when(job.isAnalyzed()).thenReturn(false);

        when(jobRepository.findById(50L)).thenReturn(Optional.of(job));
        when(recruitmentRepository.findRecruitmentByUser(user)).thenReturn(Optional.of(recruitment));
        when(htmlParserService.parseHtml(any())).thenReturn("parsed text");

        jobService.analyzeJob(50L, user);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertTrue(eventCaptor.getValue() instanceof JobAnalysisRequestedEvent);
        JobAnalysisRequestedEvent event = (JobAnalysisRequestedEvent) eventCaptor.getValue();
        assertEquals(50L, event.request().getJobId());
        assertEquals(10L, event.request().getUserId());
    }

    @Test
    @DisplayName("ResumeServiceImpl.createResume publishes ResumeAnalysisRequestedEvent when AI is enabled")
    void testResumeServiceCreateResumePublishesEvent() throws Exception {
        User user = new User();
        user.setId(15L);

        MockMultipartFile file = new MockMultipartFile("file", "my-cv.pdf", "application/pdf", "CV Content".getBytes());
        ResumeUploadDTO dto = new ResumeUploadDTO(file, true);

        when(resumeRepository.countResumesByUser_Id(15L)).thenReturn(1);
        when(fileService.toByteArray(any())).thenReturn("CV Content".getBytes());
        when(fileService.extractTextFromFile(any())).thenReturn("Extracted CV Text");
        when(fileService.cleanText("Extracted CV Text")).thenReturn("Cleaned CV Text");
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        resumeService.createResume(dto, user);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertTrue(eventCaptor.getValue() instanceof ResumeAnalysisRequestedEvent);
        ResumeAnalysisRequestedEvent event = (ResumeAnalysisRequestedEvent) eventCaptor.getValue();
        ResumeParsingMessage msg = event.message();
        assertEquals(15L, msg.userId());
        assertEquals("Cleaned CV Text", msg.rawText());
    }

    @Test
    @DisplayName("ResumeServiceImpl.analyzeResume publishes ResumeAnalysisRequestedEvent")
    void testResumeServiceAnalyzeResumePublishesEvent() {
        User user = new User();
        user.setId(15L);
        user.setEmail(new EmailAddress("candidate@test.com"));

        Resume resume = new Resume();
        resume.setId(88L);
        resume.setUser(user);
        resume.setRawText("Existing CV Text");

        when(resumeRepository.findById(88L)).thenReturn(Optional.of(resume));

        resumeService.analyzeResume(88L, user);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertTrue(eventCaptor.getValue() instanceof ResumeAnalysisRequestedEvent);
        ResumeAnalysisRequestedEvent event = (ResumeAnalysisRequestedEvent) eventCaptor.getValue();
        assertEquals(15L, event.message().userId());
        assertEquals(88L, event.message().cvId());
        assertEquals("Existing CV Text", event.message().rawText());
    }

    @Test
    @DisplayName("JobApplicationServiceImpl.applyWithUploadCv publishes ResumeAnalysisRequestedEvent")
    void testJobApplicationServiceApplyWithUploadCvPublishesEvent() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setEmail(new EmailAddress("candidate@test.com"));

        Job job = new Job();
        job.setId(800L);

        MockMultipartFile file = new MockMultipartFile("cvFile", "resume.pdf", "application/pdf", "test resume".getBytes());
        ApplyCvWithUploadRequest request = new ApplyCvWithUploadRequest(800L, file, null);

        when(userRepository.findByEmail_Value("candidate@test.com")).thenReturn(Optional.of(user));
        when(jobRepository.findById(800L)).thenReturn(Optional.of(job));
        when(jobApplicationRepository.findByJobAndUser("candidate@test.com", 800L)).thenReturn(Optional.empty());
        when(resumeRepository.countActiveByUserEmail("candidate@test.com")).thenReturn(0L);
        when(fileService.toByteArray(any())).thenReturn("bytes".getBytes());
        when(fileService.extractTextFromFile(any())).thenReturn("Parsed raw text");
        when(fileService.cleanText("Parsed raw text")).thenReturn("Cleaned raw text");
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> {
            Resume r = inv.getArgument(0);
            r.setId(555L);
            return r;
        });

        jobApplicationService.applyWithUploadCv(request, user);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertTrue(eventCaptor.getValue() instanceof ResumeAnalysisRequestedEvent);
        ResumeAnalysisRequestedEvent event = (ResumeAnalysisRequestedEvent) eventCaptor.getValue();
        assertEquals(7L, event.message().userId());
        assertEquals(555L, event.message().cvId());
        assertEquals("Cleaned raw text", event.message().rawText());
    }
}
