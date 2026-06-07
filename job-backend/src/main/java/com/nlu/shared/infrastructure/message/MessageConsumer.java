package com.nlu.shared.infrastructure.message;

import com.nlu.applicationProcess.api.dto.client.ResumeParsingMessage;
import com.nlu.applicationProcess.api.dto.client.ResumeRequest;
import com.nlu.shared.api.message.dto.ApiMessage;
import com.nlu.shared.api.message.dto.CloudUploadMessage;
import com.nlu.applicationProcess.application.ResumeParsingService;
import com.nlu.applicationProcess.application.VectorizationClient;
import com.nlu.shared.application.CloudStorageService;
import com.nlu.shared.application.SseEmitterService;
import com.nlu.shared.domain.model.SseMessagePayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.nlu.shared.api.message.dto.MailMessage;
import com.nlu.shared.application.MailService;

import lombok.AllArgsConstructor;

@Slf4j
@AllArgsConstructor
@Component
public class MessageConsumer {

    private final MailService mailService;
    private final ResumeParsingService resumeParsingService;
    private final VectorizationClient vectorizationClient;
    private final SseEmitterService sseEmitterService;

    @RabbitListener(queues = "mailQueue")
    public void receiveMail(@Payload MailMessage message) {
        mailService.sendMessage(message.getTo(), message.getSubject(), message.getContent());
    }

    @RabbitListener(queues = "parsingQueue")
    public void parsingRawText(@Payload ResumeParsingMessage message) {
        try {
            sseEmitterService.sendEvent(message.userId(), "resume-process",
                    SseMessagePayload.builder()
                            .id(message.cvId())
                            .status("analyzing")
                            .message("AI is analyzing your resume...")
                            .build());
            var res = resumeParsingService.processResume(message.rawText());
            log.info(res.toString());
            vectorizationClient.vectorizeCv(new ResumeRequest(message.userId(), message.cvId(), res));
            sseEmitterService.sendEvent(message.userId(), "resume-process",
                    SseMessagePayload.builder()
                            .id(message.cvId())
                            .status("analyzed")
                            .message("Resume analysis complete")
                            .build());

        } catch (Exception e) {
            log.error("Error processing resume: {}", message.cvId(), e);
            sseEmitterService.sendEvent(message.userId(), "resume-process", SseMessagePayload.builder()
                    .id(message.cvId())
                    .status("failed")
                    .message("Resume analysis failed")
                    .build());
        }
    }

    @RabbitListener(queues = "apiQueue")
    public void processApiService(@Payload ApiMessage message) {
        try {
            vectorizationClient.vectorizeJd(message.getVectorizeJdRequest());
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
