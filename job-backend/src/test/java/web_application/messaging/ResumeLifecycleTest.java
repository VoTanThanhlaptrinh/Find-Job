package web_application.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.client.ResumeRequest;
import com.nlu.applicationProcess.api.dto.req.ResumeDetailDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeUploadDTO;
import com.nlu.applicationProcess.api.dto.req.ResumeView;
import com.nlu.applicationProcess.application.ResumeParsingService;
import com.nlu.applicationProcess.application.VectorizationClient;
import com.nlu.applicationProcess.application.impl.ResumeServiceImpl;
import com.nlu.applicationProcess.domain.event.ResumeAnalysisRequestedEvent;
import com.nlu.applicationProcess.domain.model.QResume;
import com.nlu.applicationProcess.domain.model.Resume;
import com.nlu.applicationProcess.domain.model.ResumeStatus;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.infrastructure.message.ResumeParsingConsumer;
import com.nlu.applicationProcess.infrastructure.query.ResumeQueryDSL;
import com.nlu.identity.domain.vo.EmailAddress;
import com.nlu.identity.domain.model.User;
import com.nlu.applicationProcess.api.dto.client.ResumeModel;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.application.FileService;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.domain.exception.BadRequestException;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import com.nlu.shared.utils.MessageUtils;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResumeLifecycleTest {

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private ResumeQueryDSL resumeQueryDSL;
    @Mock
    private FileService fileService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private CloudStorageService cloudStorageService;
    @Mock
    private SseEmitterService sseEmitterService;
    @Mock
    private ResumeParsingService resumeParsingService;
    @Mock
    private VectorizationClient vectorizationClient;
    @Mock
    private MessageSource messageSource;

    private ResumeServiceImpl resumeService;
    private ResumeParsingConsumer resumeParsingConsumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        lenient().when(messageSource.getMessage(anyString(), any(), anyString(), any())).thenAnswer(inv -> inv.getArgument(2));
        new MessageUtils(messageSource);

        resumeService = new ResumeServiceImpl(
                resumeRepository,
                resumeQueryDSL,
                fileService,
                eventPublisher,
                null,
                sseEmitterService,
                cloudStorageService
        );

        resumeParsingConsumer = new ResumeParsingConsumer(
                resumeParsingService,
                vectorizationClient,
                sseEmitterService,
                resumeRepository
        );

        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("1. Resume mới mặc định là UPLOADED")
    void testNewResumeDefaultStatusIsUploaded() {
        Resume cv = new Resume();
        assertEquals(ResumeStatus.UPLOADED, cv.getStatus());
        assertFalse(cv.isAnalyzed());

        // Domain transition test
        cv.startAnalysis();
        assertEquals(ResumeStatus.ANALYZING, cv.getStatus());
        assertFalse(cv.isAnalyzed());

        cv.markReady();
        assertEquals(ResumeStatus.READY, cv.getStatus());
        assertTrue(cv.isAnalyzed());

        // Invalid transitions
        assertThrows(IllegalStateException.class, cv::startAnalysis, "Cannot startAnalysis from READY");
        assertThrows(IllegalStateException.class, cv::markAnalysisFailed, "Cannot fail from READY");
    }

    @Test
    @DisplayName("2. Upload với enableAiAnalysis=false: không extract, không publish event, status=UPLOADED")
    void testUploadWithAiDisabled() throws Exception {
        User user = new User();
        user.setId(10L);

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "dummy pdf content".getBytes());
        ResumeUploadDTO uploadDTO = new ResumeUploadDTO(file, false);

        when(resumeRepository.countResumesByUser_Id(10L)).thenReturn(0);
        when(fileService.toByteArray(any())).thenReturn("dummy pdf content".getBytes());
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeView result = resumeService.createResume(uploadDTO, user);

        // Verify no text extraction
        verify(fileService, never()).extractTextFromFile(any());
        verify(fileService, never()).cleanText(any());

        // Verify no event published
        verify(eventPublisher, never()).publishEvent(any());

        // Verify status is UPLOADED and isAnalyzed is false
        assertEquals(ResumeStatus.UPLOADED, result.status());
        assertFalse(result.isAnalyzed());
    }

    @Test
    @DisplayName("3. Upload với enableAiAnalysis=true: có extract, có publish event, status=ANALYZING")
    void testUploadWithAiEnabled() throws Exception {
        User user = new User();
        user.setId(10L);

        MockMultipartFile file = new MockMultipartFile("file", "cv.pdf", "application/pdf", "dummy pdf content".getBytes());
        ResumeUploadDTO uploadDTO = new ResumeUploadDTO(file, true);

        when(resumeRepository.countResumesByUser_Id(10L)).thenReturn(0);
        when(fileService.toByteArray(any())).thenReturn("dummy pdf content".getBytes());
        when(fileService.extractTextFromFile(any())).thenReturn("Extracted raw text");
        when(fileService.cleanText("Extracted raw text")).thenReturn("Cleaned raw text");
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        ResumeView result = resumeService.createResume(uploadDTO, user);

        // Verify text extraction
        verify(fileService).extractTextFromFile(any());
        verify(fileService).cleanText("Extracted raw text");

        // Verify event published
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertTrue(eventCaptor.getValue() instanceof ResumeAnalysisRequestedEvent);
        ResumeAnalysisRequestedEvent event = (ResumeAnalysisRequestedEvent) eventCaptor.getValue();
        assertEquals("Cleaned raw text", event.message().rawText());

        // Verify status is ANALYZING and not yet READY
        assertEquals(ResumeStatus.ANALYZING, result.status());
        assertFalse(result.isAnalyzed());
    }

    @Test
    @DisplayName("4. Consumer thành công chuyển ANALYZING → READY")
    void testConsumerSuccessTransitionsAnalyzingToReady() {
        Resume cv = new Resume();
        cv.startAnalysis(); // status is ANALYZING
        assertEquals(ResumeStatus.ANALYZING, cv.getStatus());

        when(resumeRepository.findById(55L)).thenReturn(Optional.of(cv));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        ResumeModel mockProfile = new ResumeModel(2, "Developer", "HCM", "Java", "ABC Corp");
        when(resumeParsingService.processResume("sample text")).thenReturn(mockProfile);

        ResumeParsingMessage message = new ResumeParsingMessage("sample text", 10L, 55L);
        resumeParsingConsumer.parsingRawText(message);

        verify(resumeParsingService).processResume("sample text");
        verify(vectorizationClient).vectorizeCv(any(ResumeRequest.class));

        assertEquals(ResumeStatus.READY, cv.getStatus());
        assertTrue(cv.isAnalyzed());
    }

    @Test
    @DisplayName("5. Lỗi parsing/vectorization chuyển sang ANALYSIS_FAILED và exception vẫn được rethrow")
    void testConsumerFailureTransitionsToAnalysisFailedAndRethrows() {
        // 5a. Parsing error
        Resume cv1 = new Resume();
        cv1.startAnalysis();
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(cv1));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeParsingService.processResume("err")).thenThrow(new RuntimeException("Parsing failed"));

        ResumeParsingMessage msg1 = new ResumeParsingMessage("err", 10L, 1L);
        RuntimeException ex1 = assertThrows(RuntimeException.class, () -> resumeParsingConsumer.parsingRawText(msg1));
        assertTrue(ex1.getMessage().contains("Parsing failed") || ex1.getCause().getMessage().contains("Parsing failed"));
        assertEquals(ResumeStatus.ANALYSIS_FAILED, cv1.getStatus());
        assertFalse(cv1.isAnalyzed());

        // 5b. Vectorization error
        Resume cv2 = new Resume();
        cv2.startAnalysis();
        when(resumeRepository.findById(2L)).thenReturn(Optional.of(cv2));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeParsingService.processResume("ok")).thenReturn(new ResumeModel(2, "Developer", "HCM", "Java", "ABC Corp"));
        doThrow(new RuntimeException("Vectorize failed")).when(vectorizationClient).vectorizeCv(any());

        ResumeParsingMessage msg2 = new ResumeParsingMessage("ok", 10L, 2L);
        RuntimeException ex2 = assertThrows(RuntimeException.class, () -> resumeParsingConsumer.parsingRawText(msg2));
        assertTrue(ex2.getMessage().contains("Vectorize failed") || ex2.getCause().getMessage().contains("Vectorize failed"));
        assertEquals(ResumeStatus.ANALYSIS_FAILED, cv2.getStatus());
        assertFalse(cv2.isAnalyzed());
    }

    @Test
    @DisplayName("6. Exception SSE không đổi trạng thái và không làm consumer thất bại")
    void testSseExceptionDoesNotFailConsumerOrCorruptStatus() {
        Resume cv = new Resume();
        cv.startAnalysis();
        when(resumeRepository.findById(77L)).thenReturn(Optional.of(cv));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));
        when(resumeParsingService.processResume("text")).thenReturn(new ResumeModel(2, "Developer", "HCM", "Java", "ABC Corp"));

        // Throw on every SSE call
        doThrow(new RuntimeException("SSE network connection down"))
                .when(sseEmitterService).sendEvent(anyLong(), anyString(), any());

        ResumeParsingMessage message = new ResumeParsingMessage("text", 10L, 77L);

        // Consumer must NOT throw despite SSE failure
        assertDoesNotThrow(() -> resumeParsingConsumer.parsingRawText(message));

        // Status must be READY successfully
        assertEquals(ResumeStatus.READY, cv.getStatus());
        assertTrue(cv.isAnalyzed());
    }

    @Test
    @DisplayName("7. Resume READY không bị phân tích lại khi nhận message trùng")
    void testReadyResumeSkipsDuplicateMessage() {
        Resume cv = new Resume();
        cv.startAnalysis();
        cv.markReady();
        assertEquals(ResumeStatus.READY, cv.getStatus());

        when(resumeRepository.findById(88L)).thenReturn(Optional.of(cv));

        ResumeParsingMessage duplicateMessage = new ResumeParsingMessage("text", 10L, 88L);
        assertDoesNotThrow(() -> resumeParsingConsumer.parsingRawText(duplicateMessage));

        verify(resumeParsingService, never()).processResume(any());
        verify(vectorizationClient, never()).vectorizeCv(any());
        verify(resumeRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. ANALYSIS_FAILED có thể yêu cầu phân tích lại")
    void testAnalysisFailedCanBeReanalyzed() {
        // Domain model state transition: ANALYSIS_FAILED -> ANALYZING
        Resume cv = new Resume();
        cv.startAnalysis();
        cv.markAnalysisFailed();
        assertEquals(ResumeStatus.ANALYSIS_FAILED, cv.getStatus());

        // Can transition back to ANALYZING
        cv.startAnalysis();
        assertEquals(ResumeStatus.ANALYZING, cv.getStatus());

        // Test analyzeResume service method on ANALYSIS_FAILED
        User user = new User();
        user.setId(5L);
        user.setEmail(new EmailAddress("user@test.com"));

        Resume failedCv = new Resume();
        failedCv.setId(99L);
        failedCv.setUser(user);
        failedCv.setRawText("Existing valid text");
        failedCv.startAnalysis();
        failedCv.markAnalysisFailed();

        when(resumeRepository.findById(99L)).thenReturn(Optional.of(failedCv));
        when(resumeRepository.save(any(Resume.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> resumeService.analyzeResume(99L, user));

        assertEquals(ResumeStatus.ANALYZING, failedCv.getStatus());
        verify(eventPublisher).publishEvent(any(ResumeAnalysisRequestedEvent.class));

        // Test that READY resumes are rejected from re-analysis
        failedCv.markReady();
        assertThrows(BadRequestException.class, () -> resumeService.analyzeResume(99L, user));
    }

    @Test
    @DisplayName("9. Query analyzed chỉ trả Resume READY")
    void testQueryAnalyzedResumesFiltersByReadyStatus() {
        JPAQueryFactory queryFactory = mock(JPAQueryFactory.class);
        ResumeQueryDSL queryDSL = new ResumeQueryDSL(queryFactory);

        @SuppressWarnings("unchecked")
        JPAQuery<ResumeView> jpaQuery = mock(JPAQuery.class);
        when(queryFactory.select(any(Expression.class))).thenReturn(jpaQuery);
        when(jpaQuery.from(any(QResume.class))).thenReturn(jpaQuery);
        when(jpaQuery.where(any(Predicate.class))).thenReturn(jpaQuery);
        when(jpaQuery.orderBy(any(OrderSpecifier.class))).thenReturn(jpaQuery);
        when(jpaQuery.fetch()).thenReturn(Collections.emptyList());

        List<ResumeView> result = queryDSL.getAnalyzedResumesOfUser("test@test.com");
        assertNotNull(result);

        ArgumentCaptor<Predicate> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);
        verify(jpaQuery).where(predicateCaptor.capture());

        String predicateString = predicateCaptor.getValue().toString();
        // Verifies the query uses status == READY and not isAnalyzed
        assertTrue(predicateString.contains("READY"), "Query must filter by ResumeStatus.READY");
    }

    @Test
    @DisplayName("10. API vẫn trả isAnalyzed=true khi status là READY")
    void testSerializationMaintainsIsAnalyzedCompatibility() throws Exception {
        LocalDateTime now = LocalDateTime.of(2026, 9, 13, 12, 0);

        // ResumeView with READY
        ResumeView viewReady = new ResumeView(101L, "ready.pdf", now, ResumeStatus.READY);
        String jsonReady = objectMapper.writeValueAsString(viewReady);
        JsonNode nodeReady = objectMapper.readTree(jsonReady);

        assertEquals("READY", nodeReady.get("status").asText());
        assertTrue(nodeReady.get("isAnalyzed").asBoolean());
        assertEquals(101L, nodeReady.get("id").asLong());
        assertEquals("ready.pdf", nodeReady.get("fileName").asText());

        // ResumeView with UPLOADED
        ResumeView viewUploaded = new ResumeView(102L, "uploaded.pdf", now, ResumeStatus.UPLOADED);
        String jsonUploaded = objectMapper.writeValueAsString(viewUploaded);
        JsonNode nodeUploaded = objectMapper.readTree(jsonUploaded);

        assertEquals("UPLOADED", nodeUploaded.get("status").asText());
        assertFalse(nodeUploaded.get("isAnalyzed").asBoolean());

        // ResumeView with ANALYSIS_FAILED
        ResumeView viewFailed = new ResumeView(103L, "failed.pdf", now, ResumeStatus.ANALYSIS_FAILED);
        String jsonFailed = objectMapper.writeValueAsString(viewFailed);
        JsonNode nodeFailed = objectMapper.readTree(jsonFailed);

        assertEquals("ANALYSIS_FAILED", nodeFailed.get("status").asText());
        assertFalse(nodeFailed.get("isAnalyzed").asBoolean());

        // ResumeDetailDTO with READY
        ResumeDetailDTO detailReady = new ResumeDetailDTO(201L, "detail_ready.pdf", now, ResumeStatus.READY);
        String jsonDetail = objectMapper.writeValueAsString(detailReady);
        JsonNode nodeDetail = objectMapper.readTree(jsonDetail);

        assertEquals("READY", nodeDetail.get("status").asText());
        assertTrue(nodeDetail.get("isAnalyzed").asBoolean());

        // ResumeDetailDTO with ANALYZING
        ResumeDetailDTO detailAnalyzing = new ResumeDetailDTO(202L, "detail_analyzing.pdf", now, ResumeStatus.ANALYZING);
        String jsonDetailAnalyzing = objectMapper.writeValueAsString(detailAnalyzing);
        JsonNode nodeDetailAnalyzing = objectMapper.readTree(jsonDetailAnalyzing);

        assertEquals("ANALYZING", nodeDetailAnalyzing.get("status").asText());
        assertFalse(nodeDetailAnalyzing.get("isAnalyzed").asBoolean());
    }
}
