package com.job_web.dto.ai;

import dev.langchain4j.model.output.structured.Description;

public record ResumeModel(
        String fullName,

        @Description("Tổng số năm kinh nghiệm làm việc chuyên nghiệp được tính toán từ CV. Bắt buộc phải là SỐ NGUYÊN. Ví dụ: 0 (nếu chưa có kinh nghiệm hoặc dưới 1 năm), 1, 2, 3... Hãy làm tròn số.")
        Integer yearOfExperience,

        @Description("Khối văn bản tổng hợp tất cả kỹ năng và dự án cá nhân. Tập trung vào công nghệ và cách áp dụng chúng.")
        String skillsAndProjectsContext,

        @Description("Khối văn bản tổng hợp chi tiết kinh nghiệm làm việc chuyên nghiệp (công ty, vị trí, trách nhiệm).")
        String experienceContext,

        @Description("Bản tóm tắt ngắn gọn về định hướng nghề nghiệp của ứng viên.")
        String summary
) {
}