package web_application.messaging;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.domain.event.ResumeAnalysisRequestedEvent;
import com.nlu.recruitment.api.dto.VectorizeJdRequest;
import com.nlu.recruitment.domain.event.JobAnalysisRequestedEvent;
import com.nlu.shared.infrastructure.message.MessageProducer;
import com.nlu.shared.infrastructure.message.RabbitMqEventBridge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitMqEventBridgeTest {

    @Mock
    private MessageProducer messageProducer;

    @InjectMocks
    private RabbitMqEventBridge rabbitMqEventBridge;

    @Test
    @DisplayName("Verify RabbitMqEventBridge forwards JobAnalysisRequestedEvent to MessageProducer")
    void testForwardJobAnalysisRequestedEvent() {
        VectorizeJdRequest request = new VectorizeJdRequest();
        request.setJobId(123L);
        JobAnalysisRequestedEvent event = new JobAnalysisRequestedEvent(request);

        rabbitMqEventBridge.handleJobAnalysisRequested(event);

        verify(messageProducer).processJdVectorize(request);
    }

    @Test
    @DisplayName("Verify RabbitMqEventBridge forwards ResumeAnalysisRequestedEvent to MessageProducer")
    void testForwardResumeAnalysisRequestedEvent() {
        ResumeParsingMessage message = new ResumeParsingMessage("Raw text", 10L, 20L);
        ResumeAnalysisRequestedEvent event = new ResumeAnalysisRequestedEvent(message);

        rabbitMqEventBridge.handleResumeAnalysisRequested(event);

        verify(messageProducer).processAI(message);
    }

    @Test
    @DisplayName("Verify event listeners are annotated with @TransactionalEventListener(phase = AFTER_COMMIT) and no @Async")
    void testBridgeListenerAnnotations() throws NoSuchMethodException {
        // Class level: no @Async
        assertNull(RabbitMqEventBridge.class.getAnnotation(Async.class), "RabbitMqEventBridge must not be annotated with @Async");

        // handleJobAnalysisRequested
        Method jobMethod = RabbitMqEventBridge.class.getMethod("handleJobAnalysisRequested", JobAnalysisRequestedEvent.class);
        TransactionalEventListener jobAnnotation = jobMethod.getAnnotation(TransactionalEventListener.class);
        assertNotNull(jobAnnotation, "handleJobAnalysisRequested must have @TransactionalEventListener");
        assertEquals(TransactionPhase.AFTER_COMMIT, jobAnnotation.phase(), "Listener phase must be AFTER_COMMIT");
        assertNull(jobMethod.getAnnotation(Async.class), "handleJobAnalysisRequested must not have @Async");

        // handleResumeAnalysisRequested
        Method resumeMethod = RabbitMqEventBridge.class.getMethod("handleResumeAnalysisRequested", ResumeAnalysisRequestedEvent.class);
        TransactionalEventListener resumeAnnotation = resumeMethod.getAnnotation(TransactionalEventListener.class);
        assertNotNull(resumeAnnotation, "handleResumeAnalysisRequested must have @TransactionalEventListener");
        assertEquals(TransactionPhase.AFTER_COMMIT, resumeAnnotation.phase(), "Listener phase must be AFTER_COMMIT");
        assertNull(resumeMethod.getAnnotation(Async.class), "handleResumeAnalysisRequested must not have @Async");
    }
}
