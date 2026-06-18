package com.nlu.recruitment.mapper;

import com.nlu.recruitment.api.dto.HirerJobPostView;
import com.nlu.recruitment.api.dto.JobCardView;
import com.nlu.recruitment.api.dto.JobDetailView;
import com.nlu.recruitment.api.dto.JobResponse;
import com.nlu.recruitment.domain.model.Job;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class JobViewMapper {
    public JobViewMapper() {
    }

    public JobCardView toJobCardView(Job job) {
        return new JobCardView(
                job.getId(),
                job.getTitle(),
                extractCity(job),
                job.getSalary(),
                job.getTime()
        );
    }

    public JobDetailView toJobDetailView(Job job) {
        String expiredDate = job.getExpiredDate() == null
                ? ""
                : job.getExpiredDate().atOffset(ZoneOffset.UTC).toLocalDate().toString();
        return new JobDetailView(
                job.getId(),
                job.getTitle(),
                extractStreet(job),
                job.getDescription(),
                job.getSalary(),
                job.getTime().toString(),
                job.getRequireDetails(),
                job.getSkill(),
                expiredDate,
                job.getHeadcount()
        );
    }

    public HirerJobPostView toHirerJobPostView(JobResponse job) {
        return new HirerJobPostView(
                job.getId(),
                job.getTitle(),
                job.getDescription(),
                job.getAddress(),
                job.getSalary(),
                job.getTime().name(),
                job.getApplies(),
                job.getHeadcount(),
                job.isAnalyzed()
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
