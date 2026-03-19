package com.job_web.dto.job;

import com.job_web.models.Job;
import org.springframework.data.domain.Page;

import java.time.ZoneOffset;

public final class JobViewMapper {
    private JobViewMapper() {
    }

    public static PagedPayload<JobCardView> toPagedJobCardView(Page<Job> page) {
        return new PagedPayload<>(page.map(JobViewMapper::toJobCardView).getContent());
    }

    public static JobCardView toJobCardView(Job job) {
        return new JobCardView(
                job.getId(),
                job.getTitle(),
                job.getAddress(),
                job.getDescription(),
                job.getSalary(),
                job.getTime()
        );
    }

    public static JobDetailView toJobDetailView(Job job) {
        String expiredDate = job.getExpiredDate() == null
                ? ""
                : job.getExpiredDate().atOffset(ZoneOffset.UTC).toLocalDate().toString();
        return new JobDetailView(
                job.getId(),
                job.getTitle(),
                job.getAddress(),
                job.getDescription(),
                job.getSalary(),
                job.getTime(),
                job.getRequireDetails(),
                job.getSkill(),
                expiredDate
        );
    }

    public static HirerJobPostView toHirerJobPostView(JobResponse job) {
        return new HirerJobPostView(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getAddress(),
                job.getSalary(),
                job.getTime(),
                job.getApplies()
        );
    }
}
