package com.job_web.service.ai;

import com.job_web.dto.ai.ResumeModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResumeParserAgent {
    @SystemMessage("""
        Bạn là một chuyên gia phân tích hồ sơ ứng viên (Resume Parser). 
        Nhiệm vụ của bạn là đọc văn bản thô từ CV và chuyển đổi sang định dạng JSON chính xác.
        {{cv_text}}
        Quy tắc:
        1. Technical Skills: Chỉ liệt kê các công nghệ, ngôn ngữ lập trình, framework (ví dụ: Java, .NET, SQL Server).
        2. Nếu thông tin nào không có, hãy để giá trị null hoặc danh sách rỗng.
        3. Đảm bảo giữ nguyên các thuật ngữ kỹ thuật bằng tiếng Anh.
        4. Tóm tắt phần 'summary' một cách chuyên nghiệp để phục vụ bài toán tìm kiếm ngữ nghĩa.
        
        Ghi chú/Phản hồi từ lần kiểm tra trước (Nếu có, hãy đọc kỹ để không lặp lại lỗi):
        {{feedback}}
        """)
    ResumeModel parse(@V("cv_text") String rawText, @V("feedback") String feedback);
}
