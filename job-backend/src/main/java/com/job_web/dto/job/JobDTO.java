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
        @NotBlank(message = "{validation.job.name.required}")
        String jobName,

        @Positive(message = "{validation.job.address.required}")
        long addressId,

        @NotBlank(message = "{validation.job.type.required}")
        String jobType,

        @Size(max = 255)
        String salary,
        @Size(min = 0, max = 5000, message = "{validation.job.maxLength}")
        @NotBlank(message = "{validation.job.description.required}")
        String jobDescription,
        @Size(min = 0, max = 5000, message = "{validation.job.maxLength}")
        @NotBlank(message = "{validation.job.requirement.required}")
        String jobRequirement,
        @NotBlank(message = "{validation.job.skill.required}")
        @Size(min = 0, max = 5000, message = "{validation.job.maxLength}")
        String jobSkill,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        @NotNull(message = "{validation.job.deadline.required}")
        LocalDate deadlineCV,

        @Positive(message = "{validation.job.hirer.required}")
        long hirerId,

        @Size(min = 0, max = 5000, message = "{validation.job.maxLength}")
        String moreDetail,

        @Min(value = 1, message = "{validation.job.headcount.min}")
        Integer headcount
) {
    @AssertTrue(message = "{validation.job.deadline.future}")
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
        if(headcount != null && headcount > 0){
            job.setHeadcount(headcount);
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

    public Integer getHeadcount() {
        return headcount;
    }
}
