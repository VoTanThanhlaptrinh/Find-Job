package com.job_web.service.ai.impl;

import com.job_web.dto.ai.ResumeModel;
import com.job_web.dto.ai.VerificationResult;
import com.job_web.service.ai.AIService;
import com.job_web.service.ai.ResumeParserAgent;
import com.job_web.service.ai.ResumeVerifierAgent;

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
    private ResumeVerifierAgent verifierAgent;
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
        this.verifierAgent = AiServices.create(ResumeVerifierAgent.class, model);
    }

    private ResumeModel parser(String rawText, String feedback){
        if(parserAgent == null){
            parserAgent = AiServices.create(ResumeParserAgent.class, model);
        }
        return parserAgent.parse(rawText, feedback);
    }

    private VerificationResult verify(ResumeModel profile, String rawText){
        if(verifierAgent == null){
            verifierAgent = AiServices.create(ResumeVerifierAgent.class, model);
        }
        return verifierAgent.verify(profile, rawText);
    }

    @Override
    public ResumeModel processResume(String rawText) {
        var bestResume = parser(rawText, "");
        var bestVerify = verify(bestResume, rawText);
        if (bestVerify.getConfidenceScore() >= PASS_THRESHOLD) {
            return bestResume;
        }
        return bestResume;
    }
}