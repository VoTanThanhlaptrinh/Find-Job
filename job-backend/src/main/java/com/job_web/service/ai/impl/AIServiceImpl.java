package com.job_web.service.ai.impl;

import com.job_web.dto.ai.ResumeModel;
import com.job_web.dto.ai.VerificationResult;
import com.job_web.service.ai.AIService;
import com.job_web.service.ai.ResumeParserAgent;
import com.job_web.service.ai.ResumeVerifierAgent;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;

public class AIServiceImpl implements AIService {
    private final GoogleAiGeminiChatModel model;
    @Value("${application.service.impl.gemini.api-key}")
    private String apiKey;
    private ResumeParserAgent parserAgent;
    private ResumeVerifierAgent verifierAgent;
    private static final int MAX_RETRIES = 3;
    private static final int PASS_THRESHOLD = 80;
    public AIServiceImpl(){
         model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-1.5-pro")
                .temperature(0.0) // Thiết lập bằng 0 để đảm bảo tính nhất quán của dữ liệu
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
