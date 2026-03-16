package com.job_web.dto.ai;

import dev.langchain4j.model.output.structured.Description;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
public class ResumeModel {
    public String fullName;
    public String email;
    public String phone;
    @Description("Danh sách các kỹ năng cứng như Java, Spring Boot, SQL...")
    public List<String> technicalSkills;
    public List<WorkExperience> workHistory;
    public List<Certificate> certificates;
    public String education;
    @Description("Bản tóm tắt chuyên nghiệp về năng lực và định hướng của ứng viên, dùng cho tìm kiếm ngữ nghĩa.")
    public String summary;
}
@Getter
@Setter
class WorkExperience {
    public String company;
    public String position;
    public String duration;
    public String description;
}
@Getter
@Setter
class Certificate {
    public String name;
    @Description("Xếp loại, điểm số hoặc đơn vị cấp chứng chỉ")
    public String value;
}
@Getter
@Setter
class ResumeEmbeddingRequest {
    private Long userId;     // ID người dùng từ hệ thống
    private Long cvId;       // ID bản CV cụ thể (để handle việc 1 user có nhiều CV)
    private ResumeModel data; // Nội dung chuyên môn đã parse xong
}