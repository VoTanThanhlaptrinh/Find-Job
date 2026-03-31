package com.job_web.dto.job;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.job_web.models.Job;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.ZoneOffset;

public record JobDTO(
        @NotBlank(message = "Ten cong viec khong duoc rong")
        String jobName,

        @Positive(message = "Dia diem cong viec khong duoc rong")
        long addressId,

        @NotBlank(message = "Loai cong viec khong duoc rong")
        String jobType,

        @Size(max = 255)
        String salary,
        @Size(min = 0, max = 5000, message = "Do dai thong tin toi da 5000 ky tu")
        @NotBlank(message = "Mo ta cong viec khong duoc rong")
        String jobDescription,
        @Size(min = 0, max = 5000, message = "Do dai thong tin toi da 5000 ky tu")
        @NotBlank(message = "Yeu cau cong viec khong duoc rong")
        String jobRequirement,
        @NotBlank(message = "Yeu cau ve ky nang, cong cu khong duoc rong")
        @Size(min = 0, max = 5000, message = "Do dai thong tin toi da 5000 ky tu")
        String jobSkill,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @NotNull(message = "Thoi gian nop CV khong duoc rong")
        LocalDate deadlineCV,

        @Positive(message = "Can thong tin nguoi dang cong viec")
        long hirerId,

        @Size(min = 0, max = 5000, message = "Thong tin them toi da 5000 ky tu")
        String moreDetail
) {
    @AssertTrue(message = "Han nop CV phai sau it nhat mot ngay so voi hom nay")
    public boolean isDeadlineValid() {
        return deadlineCV != null && deadlineCV.isAfter(LocalDate.now());
    }

    public void updateJob(Job job) {
        job.setTime(jobType);
        job.setDescription(jobDescription);
        job.setRequireDetails(jobRequirement);
        job.setSalary(salary);
        job.setTitle(jobName);
        if(moreDetail != null && !moreDetail.isEmpty()){
            job.setMoreDetail(moreDetail);
        }
    }

    public Job toJob() {
        Job job = new Job();
        updateJob(job);
        return job;
    }

    public String getJobName() {
        return jobName;
    }

    public long getAddressId() {
        return addressId;
    }

    public String getJobType() {
        return jobType;
    }

    public String getSalary() {
        return salary;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public String getJobRequirement() {
        return jobRequirement;
    }

    public LocalDate getDeadlineCV() {
        return deadlineCV;
    }

    public long getHirerId() {
        return hirerId;
    }

    public String getMoreDetail() {
        return moreDetail;
    }
}
