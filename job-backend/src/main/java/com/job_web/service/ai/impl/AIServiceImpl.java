package com.job_web.service.ai.impl;

import com.job_web.dto.ai.ResumeModel;
import com.job_web.service.ai.AIService;
import com.job_web.service.ai.ResumeParserAgent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AIServiceImpl implements AIService {
    private  ChatModel model;
    private ResumeParserAgent parserAgent;
    private static final int MAX_RETRIES = 1;
    private static final int PASS_THRESHOLD = 80;
    @Value("${application.service.impl.deepseek.api-key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        this.model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1") // Thêm /v1 để đúng chuẩn OpenAI SDK
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .temperature(0.0)
                .logRequests(true)
                .logResponses(true)
                .build();
        this.parserAgent = AiServices.create(ResumeParserAgent.class, model);
        log.info("AI resume parser agent initialized with model: deepseek-chat");
    }

    private ResumeModel parser(String rawText){

        if(parserAgent == null){
            parserAgent = AiServices.create(ResumeParserAgent.class, model);
            log.warn("ParserAgent was null — re-initialized at runtime");
        }
        return parserAgent.parse(rawText);
    }

    @Override
    public ResumeModel processResume(String rawText) {
        // MDC userId/cvId is already set by the caller (message consumer).
        // rawText may contain PII (name, email, phone) — never log its content.
        log.info("Starting AI resume parsing — input length: {} chars", rawText.length());

        long startTime = System.currentTimeMillis();
        ResumeModel result = parser(rawText);
        long duration = System.currentTimeMillis() - startTime;

        log.info("AI resume parsing completed in {}ms — extracted title: {}, yoe: {}",
                duration,
                result.title(),
                result.yearOfExperience());

        return result;
    }
}