package com.job_web.dto.job;

import com.job_web.models.Address;
import com.job_web.models.Job;
import org.springframework.data.domain.Page;

import java.time.ZoneOffset;

public final class JobViewMapper {
    private JobViewMapper() {
    }

    public static JobCardView toJobCardView(Job job) {
        return new JobCardView(
                job.getId(),
                job.getTitle(),
                formatAddress(job.getAddress()),
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
                formatAddress(job.getAddress()),
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

    private static String formatAddress(Address address) {
        if (address == null) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, address.getStreet());
        appendAddressPart(builder, address.getDistrict());
        appendAddressPart(builder, address.getCity());
        return builder.toString();
    }

    private static void appendAddressPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(", ");
        }
        builder.append(value);
    }
}
