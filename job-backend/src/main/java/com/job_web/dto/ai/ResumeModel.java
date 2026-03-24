package com.job_web.dto.ai;

import dev.langchain4j.model.output.structured.Description;
import java.util.List;

public record ResumeModel(
        String fullName,

        @Description("Khối văn bản tổng hợp tất cả kỹ năng và dự án cá nhân. Tập trung vào công nghệ và cách áp dụng chúng.")
        String skillsAndProjectsContext,

        @Description("Khối văn bản tổng hợp chi tiết kinh nghiệm làm việc chuyên nghiệp (công ty, vị trí, trách nhiệm).")
        String experienceContext,

        @Description("Bản tóm tắt ngắn gọn về định hướng nghề nghiệp của ứng viên.")
        String summary
) {
}

record ResumeEmbeddingRequest(
        Long userId,
        Long cvId,
        ResumeModel data
) {
}