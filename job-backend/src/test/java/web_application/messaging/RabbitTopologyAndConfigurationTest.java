package web_application.messaging;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.infrastructure.message.ResumeParsingConsumer;
import com.nlu.recruitment.api.dto.VectorizeJdRequest;
import com.nlu.recruitment.infrastructure.message.JobVectorizationConsumer;
import com.nlu.shared.api.message.dto.ApiMessage;
import com.nlu.shared.api.message.dto.MailMessage;
import com.nlu.shared.infrastructure.config.RabbitMQConfig;
import com.nlu.shared.infrastructure.message.MailConsumer;
import com.nlu.shared.infrastructure.message.MessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RabbitTopologyAndConfigurationTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MessageProducer messageProducer;

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    @DisplayName("Verify v2 queues and exchanges are durable, and cloudUploadQueue is unchanged")
    void testTopologyDurability() {
        // 1. Mail queue & exchange
        Queue mailQueue = (Queue) invokeMethod("mailQueue");
        assertEquals("mailQueue.v2", mailQueue.getName());
        assertTrue(mailQueue.isDurable(), "mailQueue.v2 must be durable");

        DirectExchange mailExchange = (DirectExchange) invokeMethod("mailExchange");
        assertEquals("mailExchange.v2", mailExchange.getName());
        assertTrue(mailExchange.isDurable(), "mailExchange.v2 must be durable");

        Binding mailBinding = (Binding) invokeMethodWithArgs("mailBinding", new Class<?>[]{Queue.class, DirectExchange.class}, mailQueue, mailExchange);
        assertEquals("mailRoutingKey.v2", mailBinding.getRoutingKey());

        // 2. Resume parsing queue & exchange
        Queue parsingQueue = (Queue) invokeMethod("parsingQueue");
        assertEquals("parsingQueue.v2", parsingQueue.getName());
        assertTrue(parsingQueue.isDurable(), "parsingQueue.v2 must be durable");

        DirectExchange parsingExchange = (DirectExchange) invokeMethod("parsingExchange");
        assertEquals("parsingExchange.v2", parsingExchange.getName());
        assertTrue(parsingExchange.isDurable(), "parsingExchange.v2 must be durable");

        Binding parsingBinding = (Binding) invokeMethodWithArgs("parsingBinding", new Class<?>[]{Queue.class, DirectExchange.class}, parsingQueue, parsingExchange);
        assertEquals("parsingRoutingKey.v2", parsingBinding.getRoutingKey());

        // 3. Job vectorization queue & exchange
        Queue apiQueue = (Queue) invokeMethod("apiQueue");
        assertEquals("apiQueue.v2", apiQueue.getName());
        assertTrue(apiQueue.isDurable(), "apiQueue.v2 must be durable");

        DirectExchange apiExchange = (DirectExchange) invokeMethod("apiExchange");
        assertEquals("apiExchange.v2", apiExchange.getName());
        assertTrue(apiExchange.isDurable(), "apiExchange.v2 must be durable");

        Binding apiBinding = (Binding) invokeMethodWithArgs("apiBinding", new Class<?>[]{Queue.class, DirectExchange.class}, apiQueue, apiExchange);
        assertEquals("apiRoutingKey.v2", apiBinding.getRoutingKey());

        // 4. Cloud upload queue & exchange (must be unchanged and NOT durable)
        Queue cloudUploadQueue = (Queue) invokeMethod("cloudUploadQueue");
        assertEquals("cloudUploadQueue", cloudUploadQueue.getName());
        assertFalse(cloudUploadQueue.isDurable(), "cloudUploadQueue must remain non-durable");

        DirectExchange cloudUploadExchange = (DirectExchange) invokeMethod("cloudUploadExchange");
        assertEquals("cloudUploadExchange", cloudUploadExchange.getName());
    }

    @Test
    @DisplayName("Verify MessageProducer publishes to v2 topology with PERSISTENT delivery mode and original payload")
    void testProducerV2TopologyAndPersistentDeliveryMode() {
        // Mail
        MailMessage mailMessage = new MailMessage("recipient@example.com", "Subject", "Content");
        messageProducer.sendMail(mailMessage);

        ArgumentCaptor<MessagePostProcessor> mailPostProcessorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.MAIL_EXCHANGE_V2),
                eq(RabbitMQConfig.MAIL_ROUTING_KEY_V2),
                eq(mailMessage),
                mailPostProcessorCaptor.capture()
        );
        Message mailMsg = new Message("test".getBytes(), new MessageProperties());
        mailPostProcessorCaptor.getValue().postProcessMessage(mailMsg);
        assertEquals(MessageDeliveryMode.PERSISTENT, mailMsg.getMessageProperties().getDeliveryMode());

        // AI Resume Parsing
        ResumeParsingMessage parsingMessage = new ResumeParsingMessage("CV text", 100L, 200L);
        messageProducer.processAI(parsingMessage);

        ArgumentCaptor<MessagePostProcessor> aiPostProcessorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.PARSING_EXCHANGE_V2),
                eq(RabbitMQConfig.PARSING_ROUTING_KEY_V2),
                eq(parsingMessage),
                aiPostProcessorCaptor.capture()
        );
        Message aiMsg = new Message("test".getBytes(), new MessageProperties());
        aiPostProcessorCaptor.getValue().postProcessMessage(aiMsg);
        assertEquals(MessageDeliveryMode.PERSISTENT, aiMsg.getMessageProperties().getDeliveryMode());

        // Job Vectorization
        VectorizeJdRequest jdRequest = new VectorizeJdRequest();
        jdRequest.setJobId(300L);
        jdRequest.setUserId(400L);
        messageProducer.processJdVectorize(jdRequest);

        ArgumentCaptor<MessagePostProcessor> jdPostProcessorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<ApiMessage> apiMsgCaptor = ArgumentCaptor.forClass(ApiMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.API_EXCHANGE_V2),
                eq(RabbitMQConfig.API_ROUTING_KEY_V2),
                apiMsgCaptor.capture(),
                jdPostProcessorCaptor.capture()
        );
        assertEquals(300L, apiMsgCaptor.getValue().getVectorizeJdRequest().getJobId());
        Message jdMsg = new Message("test".getBytes(), new MessageProperties());
        jdPostProcessorCaptor.getValue().postProcessMessage(jdMsg);
        assertEquals(MessageDeliveryMode.PERSISTENT, jdMsg.getMessageProperties().getDeliveryMode());
    }

    @Test
    @DisplayName("Verify Consumers listen to v2 queues")
    void testConsumersListenToV2Queues() throws NoSuchMethodException {
        // MailConsumer
        Method mailMethod = MailConsumer.class.getMethod("receiveMail", MailMessage.class);
        RabbitListener mailListener = mailMethod.getAnnotation(RabbitListener.class);
        assertNotNull(mailListener);
        assertTrue(Arrays.asList(mailListener.queues()).contains("mailQueue.v2"));

        // ResumeParsingConsumer
        Method resumeMethod = ResumeParsingConsumer.class.getMethod("parsingRawText", ResumeParsingMessage.class);
        RabbitListener resumeListener = resumeMethod.getAnnotation(RabbitListener.class);
        assertNotNull(resumeListener);
        assertTrue(Arrays.asList(resumeListener.queues()).contains("parsingQueue.v2"));

        // JobVectorizationConsumer
        Method jobMethod = JobVectorizationConsumer.class.getMethod("processApiService", ApiMessage.class);
        RabbitListener jobListener = jobMethod.getAnnotation(RabbitListener.class);
        assertNotNull(jobListener);
        assertTrue(Arrays.asList(jobListener.queues()).contains("apiQueue.v2"));
    }

    @Test
    @DisplayName("Verify legacy MessageConsumer has been removed from classpath")
    void testLegacyMessageConsumerRemoved() {
        assertThrows(ClassNotFoundException.class, () -> {
            Class.forName("com.nlu.shared.infrastructure.message.MessageConsumer");
        }, "Old MessageConsumer class must be deleted");
    }

    private Object invokeMethod(String methodName) {
        try {
            Method m = RabbitMQConfig.class.getDeclaredMethod(methodName);
            m.setAccessible(true);
            return m.invoke(rabbitMQConfig);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokeMethodWithArgs(String methodName, Class<?>[] paramTypes, Object... args) {
        try {
            Method m = RabbitMQConfig.class.getDeclaredMethod(methodName, paramTypes);
            m.setAccessible(true);
            return m.invoke(rabbitMQConfig, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
