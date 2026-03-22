package com.job_web.dto.ai;

import dev.langchain4j.model.output.structured.Description;

import java.util.List;

public record ResumeModel(
        String fullName,
        String email,
        String phone,
        @Description("Danh sÃ¡ch cÃ¡c ká»¹ nÄƒng cá»©ng nhÆ° Java, Spring Boot, SQL...")
        List<String> technicalSkills,
        List<WorkExperience> workHistory,
        List<Certificate> certificates,
        String education,
        @Description("Báº£n tÃ³m táº¯t chuyÃªn nghiá»‡p vá» nÄƒng lá»±c vÃ  Ä‘á»‹nh hÆ°á»›ng cá»§a á»©ng viÃªn, dÃ¹ng cho tÃ¬m kiáº¿m ngá»¯ nghÄ©a.")
        String summary
) {
}

record WorkExperience(
        String company,
        String position,
        String duration,
        String description
) {
}

record Certificate(
        String name,
        @Description("Xáº¿p loáº¡i, Ä‘iá»ƒm sá»‘ hoáº·c Ä‘Æ¡n vá»‹ cáº¥p chá»©ng chá»‰")
        String value
) {
}

record ResumeEmbeddingRequest(
        Long userId,
        Long cvId,
        ResumeModel data
) {
}
