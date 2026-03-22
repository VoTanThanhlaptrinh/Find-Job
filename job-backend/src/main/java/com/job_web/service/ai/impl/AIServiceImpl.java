package com.job_web.service.ai.impl;

import com.job_web.dto.ai.ResumeModel;
import com.job_web.dto.ai.VerificationResult;
import com.job_web.service.ai.AIService;
import com.job_web.service.ai.ResumeParserAgent;
import com.job_web.service.ai.ResumeVerifierAgent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AIServiceImpl implements AIService {

    // Sử dụng Interface ChatLanguageModel thay vì class cụ thể để dễ mở rộng sau này
    private final ChatModel model;

    private ResumeParserAgent parserAgent;
    private ResumeVerifierAgent verifierAgent;

    private static final int MAX_RETRIES = 3;
    private static final int PASS_THRESHOLD = 80;

    public AIServiceImpl(@Value("${application.service.impl.deepseek.api-key}") String apiKey) {
        this.model = OpenAiChatModel.builder()
                .baseUrl("https://api.deepseek.com/v1") // Trỏ base URL về DeepSeek
                .apiKey(apiKey)
                .modelName("deepseek-chat") // Tên model của DeepSeek
                .temperature(0.0) // Giữ nguyên 0.0 cho dữ liệu JSON nghiêm ngặt
                .logRequests(true)
                .logResponses(true)
                .build();
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

        var count = 0;
        while(count < MAX_RETRIES){
            var updateResume = parser(rawText, bestVerify.getSuggestion());
            count++;
            var currentVerify = verify(updateResume,rawText);

            if (currentVerify.getConfidenceScore() > bestVerify.getConfidenceScore()) {
                bestVerify = currentVerify;
                bestResume = updateResume;
            }
            if (bestVerify.getConfidenceScore() >= PASS_THRESHOLD) {
                break;
            }
        }
        return bestResume;
    }
}