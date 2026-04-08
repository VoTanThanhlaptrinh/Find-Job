package com.job_web.dto.ai;

import dev.langchain4j.model.output.structured.Description;

public record ResumeModel(
        @Description("Tổng số năm kinh nghiệm làm việc chuyên nghiệp được tính toán từ CV. Bắt buộc phải là SỐ NGUYÊN. Ví dụ: 0 (nếu chưa có kinh nghiệm hoặc dưới 1 năm), 1, 2, 3... Hãy làm tròn số.")
        Integer yearOfExperience,

        @Description("Tên chức danh/vị trí mong muốn của ứng viên được ghi trong CV (ví dụ: Backend Developer, Kế toán trưởng, Marketing Executive). Nếu không tìm thấy, trả về null.")
        String title,

        @Description("Thành phố hoặc tỉnh nơi ứng viên đang sinh sống hoặc mong muốn làm việc (ví dụ: Hồ Chí Minh, Hà Nội, Đà Nẵng). Chỉ lấy tên thành phố/tỉnh, không kèm địa chỉ chi tiết. Nếu không tìm thấy, trả về null.")
        String city,

        @Description("Khối văn bản tổng hợp tất cả kỹ năng và dự án cá nhân. Tập trung vào công nghệ và cách áp dụng chúng.")
        String skillsAndProjectsContext,

        @Description("Khối văn bản tổng hợp chi tiết kinh nghiệm làm việc chuyên nghiệp (công ty, vị trí, trách nhiệm).")
        String experienceContext
) {
}