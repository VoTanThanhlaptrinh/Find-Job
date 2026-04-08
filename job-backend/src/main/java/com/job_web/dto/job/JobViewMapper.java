package com.job_web.dto.job;

import com.job_web.models.Job;

import java.time.ZoneOffset;

public final class JobViewMapper {
    private JobViewMapper() {
    }

    public static JobCardView toJobCardView(Job job) {
        return new JobCardView(
                job.getId(),
                job.getTitle(),
                extractCity(job),
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
                extractStreet(job),
                job.getDescription(),
                job.getSalary(),
                job.getTime(),
                job.getRequireDetails(),
                job.getSkill(),
                expiredDate,
                job.getHeadcount()
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
                job.getApplies(),
                job.getHeadcount()
        );
    }

    private static String extractCity(Job job) {
        if (job.getAddress() == null || job.getAddress().getCity() == null) {
            return "";
        }
        return job.getAddress().getCity();
    }

    private static String extractStreet(Job job) {
        if (job.getAddress() == null || job.getAddress().getStreet() == null) {
            return "";
        }
        return job.getAddress().getStreet();
    }
}
