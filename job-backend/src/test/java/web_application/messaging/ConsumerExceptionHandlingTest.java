package web_application.messaging;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.application.ResumeParsingService;
import com.nlu.applicationProcess.application.VectorizationClient;
import com.nlu.applicationProcess.domain.repository.ResumeRepository;
import com.nlu.applicationProcess.infrastructure.message.ResumeParsingConsumer;
import com.nlu.recruitment.api.dto.VectorizeJdRequest;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.recruitment.infrastructure.message.JobVectorizationConsumer;
import com.nlu.shared.api.message.dto.ApiMessage;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.domain.model.SseMessagePayload;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsumerExceptionHandlingTest {

    @Mock
    private VectorizationClient vectorizationClient;

    @Mock
    private JobRepository jobRepository;

    @Mock
    private SseEmitterService sseEmitterService;

    @Mock
    private ResumeParsingService resumeParsingService;

    @Mock
    private ResumeRepository resumeRepository;

    @InjectMocks
    private JobVectorizationConsumer jobVectorizationConsumer;

    @InjectMocks
    private ResumeParsingConsumer resumeParsingConsumer;

    @Test
    @DisplayName("JobVectorizationConsumer sends SSE failed event and rethrows exception on failure")
    void testJobVectorizationConsumerRethrowsException() {
        VectorizeJdRequest request = new VectorizeJdRequest();
        request.setJobId(101L);
        request.setUserId(202L);
        ApiMessage message = ApiMessage.vectorizeJd(request);

        RuntimeException expectedError = new RuntimeException("Vectorize service unreachable");
        doThrow(expectedError).when(vectorizationClient).vectorizeJd(request);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            jobVectorizationConsumer.processApiService(message);
        });

        assertSame(expectedError, thrown);

        // Verify SSE failed event sent
        ArgumentCaptor<SseMessagePayload> payloadCaptor = ArgumentCaptor.forClass(SseMessagePayload.class);
        verify(sseEmitterService).sendEvent(eq(202L), eq("job-process"), payloadCaptor.capture());
        assertEquals("failed", payloadCaptor.getValue().getStatus());
        assertEquals(101L, payloadCaptor.getValue().getId());
    }

    @Test
    @DisplayName("JobVectorizationConsumer: SSE failure does not mask main processing error")
    void testJobVectorizationConsumerSseFailureDoesNotMaskMainError() {
        VectorizeJdRequest request = new VectorizeJdRequest();
        request.setJobId(101L);
        request.setUserId(202L);
        ApiMessage message = ApiMessage.vectorizeJd(request);

        RuntimeException mainError = new RuntimeException("Primary vectorization error");
        doThrow(mainError).when(vectorizationClient).vectorizeJd(request);
        doThrow(new RuntimeException("SSE network broken")).when(sseEmitterService).sendEvent(anyLong(), anyString(), any());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            jobVectorizationConsumer.processApiService(message);
        });

        assertSame(mainError, thrown, "Main processing error must not be masked by SSE error");
    }

    @Test
    @DisplayName("ResumeParsingConsumer sends SSE failed event and rethrows exception on failure")
    void testResumeParsingConsumerRethrowsException() {
        ResumeParsingMessage message = new ResumeParsingMessage("Raw text", 202L, 101L);

        RuntimeException expectedError = new RuntimeException("AI parsing error");
        when(resumeParsingService.processResume("Raw text")).thenThrow(expectedError);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            resumeParsingConsumer.parsingRawText(message);
        });

        assertSame(expectedError, thrown);

        // Verify SSE failed event was sent
        ArgumentCaptor<SseMessagePayload> payloadCaptor = ArgumentCaptor.forClass(SseMessagePayload.class);
        verify(sseEmitterService, atLeastOnce()).sendEvent(eq(202L), eq("resume-process"), payloadCaptor.capture());
        
        boolean hasFailedEvent = payloadCaptor.getAllValues().stream()
                .anyMatch(p -> "failed".equals(p.getStatus()));
        assertTrue(hasFailedEvent, "SSE 'failed' event must be sent on failure");
    }

    @Test
    @DisplayName("ResumeParsingConsumer: SSE failure does not mask main processing error")
    void testResumeParsingConsumerSseFailureDoesNotMaskMainError() {
        ResumeParsingMessage message = new ResumeParsingMessage("Raw text", 202L, 101L);

        RuntimeException mainError = new RuntimeException("Primary AI parsing error");
        when(resumeParsingService.processResume("Raw text")).thenThrow(mainError);
        doAnswer(invocation -> {
            SseMessagePayload<?> payload = invocation.getArgument(2);
            if (payload != null && "failed".equals(payload.getStatus())) {
                throw new RuntimeException("SSE connection lost while sending failure event");
            }
            return null;
        }).when(sseEmitterService).sendEvent(eq(202L), eq("resume-process"), any());

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            resumeParsingConsumer.parsingRawText(message);
        });

        // The thrown exception must be the main error (or wrap it), not the SSE exception
        assertTrue(thrown == mainError || thrown.getCause() == mainError, "Main error must not be masked by SSE error");
        assertNotEquals("SSE connection lost while sending failure event", thrown.getMessage());
    }
}
