package com.job_web.service.ai;

import com.job_web.dto.ai.ResumeModel;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface ResumeParserAgent {
    @SystemMessage("""
    Bạn là một chuyên gia phân tích dữ liệu cho hệ thống Semantic Search tuyển dụng. 
    Nhiệm vụ của bạn là đọc nội dung CV gốc, tính toán các chỉ số và tổng hợp văn bản thành các khối nội dung giàu ngữ nghĩa.

    YÊU CẦU TRÍCH XUẤT:
    1. yearOfExperience (Số năm kinh nghiệm):
       - Phân tích các mốc thời gian làm việc tại các công ty/tổ chức để tính tổng số năm làm việc.
       - CHỈ tính kinh nghiệm làm việc chuyên nghiệp (Full-time, Part-time, Internship tại doanh nghiệp).
       - KHÔNG tính thời gian làm đồ án môn học hay dự án cá nhân.
       - Trả về MỘT SỐ NGUYÊN duy nhất. Làm tròn theo quy tắc toán học (vd: 1.5 năm -> 2, dưới 1 năm -> 0).

    2. skillsAndProjectsContext: 
       - Tổng hợp tất cả kỹ năng cứng và các dự án (cá nhân, đồ án môn học). 
       - Viết dưới dạng các câu mô tả ngắn gọn nhưng đủ ngữ cảnh. 
       - Ví dụ: 'Sử dụng Java và Spring Boot để xây dựng RESTful API; Thành thạo Angular và Tailwind CSS trong phát triển giao diện; Có kinh nghiệm triển khai hệ thống với Docker.'

    3. experienceContext: 
       - Tổng hợp toàn bộ quá trình làm việc tại các công ty/tổ chức. 
       - Nếu không có kinh nghiệm đi làm, hãy trả về chính xác chuỗi: 'Chưa có kinh nghiệm làm việc chuyên nghiệp'.
       - Định dạng tiêu chuẩn: 'Làm việc tại [Công ty] với vị trí [Vị trí] trong [Thời gian]. Trách nhiệm chính bao gồm: [Mô tả].'

    QUY TẮC NGHIÊM NGẶT:
    - Giữ nguyên các thuật ngữ kỹ thuật bằng tiếng Anh (ví dụ: RESTful, Database, Framework).
    - Không sử dụng các ký tự đặc biệt (như icon, emoji) làm nhiễu văn bản.
    - Đảm bảo khối văn bản có tính logic, rành mạch để model Sentence-BERT tạo Vector chính xác nhất.
    """)
    @UserMessage("""
    Hãy thực hiện trích xuất dữ liệu dựa trên nội dung CV sau đây:
    
    === NỘI DUNG CV GỐC ===
    {{cv_text}}
    =======================

    Ghi chú bổ sung / Yêu cầu sửa lỗi: 
    {{feedback}}
    """)
    ResumeModel parse(@V("cv_text") String rawText, @V("feedback") String feedback);
}