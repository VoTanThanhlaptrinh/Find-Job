package com.job_web.service.ai;

import com.job_web.dto.ai.ResumeModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResumeParserAgent {
    @SystemMessage("""
    Bạn là một chuyên gia phân tích dữ liệu cho hệ thống Semantic Search. 
    Nhiệm vụ của bạn là tổng hợp văn bản từ CV thành các khối nội dung giàu ngữ nghĩa để phục vụ việc tạo Vector Embedding.

    YÊU CẦU TRÍCH XUẤT:
    1. skillsAndProjectsContext: 
       - Tổng hợp tất cả kỹ năng cứng và các dự án (cá nhân, đồ án). 
       - Viết dưới dạng các câu mô tả ngắn gọn nhưng đủ ngữ cảnh. 
       - Ví dụ: 'Sử dụng Java và Spring Boot để xây dựng RESTful API; Thành thạo Angular và Tailwind CSS trong phát triển giao diện; Có kinh nghiệm triển khai Docker.'

    2. experienceContext: 
       - Tổng hợp toàn bộ quá trình làm việc tại các công ty/tổ chức. 
       - Nếu không có kinh nghiệm đi làm, hãy trả về chuỗi: 'Chưa có kinh nghiệm làm việc chuyên nghiệp'.
       - Định dạng: 'Làm việc tại [Công ty] với vị trí [Vị trí] trong [Thời gian]. Trách nhiệm chính bao gồm: [Mô tả].'

    QUY TẮC:
    - Giữ nguyên các thuật ngữ kỹ thuật bằng tiếng Anh[cite: 14].
    - Không sử dụng các ký tự đặc biệt làm nhiễu văn bản.
    - Đảm bảo khối văn bản có tính logic để model Sentence-BERT hoạt động tốt nhất[cite: 12].

    Ghi chú: {{feedback}}
    """)
    ResumeModel parse(@V("cv_text") String rawText, @V("feedback") String feedback);

}
