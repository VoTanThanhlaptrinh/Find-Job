package com.job_web.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.job_web.models.Hirer;
import com.job_web.models.Job;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.*;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Data
public class JobDTO {
    @NotBlank(message = "Tên công việc không được rỗng")
    private String jobName;
    @NotBlank(message = "Địa điểm công việc không được rỗng")
    private String location;
    @NotBlank(message = "Loại công việc không được rỗng")
    private String jobType;
    @Min(value = 2000000, message = "Mức lương tối thiểu là 2 triệu")
    private double salary;
    @NotBlank(message = "Tên công việc không được rỗng")
    private String jobDescription;
    @NotBlank(message = "Tên công việc không được rỗng")
    private String jobRequirement;
    @NotBlank(message = "Tên công việc không được rỗng")
    private String jobSkill;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @NotNull(message = "Thời gian nộp CV không được rỗng")
    private LocalDate deadlineCV;
    @NotBlank(message = "Tên công ty không được rỗng")
    private String companyName;
    private String companyDescription;
    private String compayWebsite;
    @NotNull(message = "Công việc cần có logo đại diện")
    private MultipartFile image;

    @AssertTrue(message = "Hạn nộp CV phải sau ít nhất một ngày so với hôm nay")
    public boolean isDeadlineValid() {
        return deadlineCV != null && deadlineCV.isAfter(LocalDate.now());
    }
    @AssertTrue(message = "Chỉ chấp nhận png hoặc jpg")
    public boolean validLogoExtension(){
        String contentType = image.getContentType();
        return contentType != null && (contentType.equals("image/jpeg") || contentType.equals("image/png"));
    }
    @AssertTrue(message = "kích thước ảnh tối đa 2mb")
    public boolean isImageSizeValid(){
        return image != null && image.getSize() <= 2 * 1024 * 1024;
    }
    public Job toJob(){
        Job job = new Job();
        job.setTime(jobType);
        job.setDescription(jobDescription);
        job.setRequireDetails(jobRequirement);
        job.setSkill(jobSkill);
        job.setAddress(location);
        job.setSalary(salary);
        job.setTitle(jobName);
        job.setExpiredDate(deadlineCV
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant());
        Hirer hirer = new Hirer();
        hirer.setCompanyName(companyName);
        hirer.setSocialLink(compayWebsite);
        job.setHirer(hirer);
        return job;
    }
}
