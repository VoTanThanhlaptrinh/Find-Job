package com.job_web.recruiment.infrastructure.query;

import com.job_web.recruiment.domain.model.Job;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class JobSpecifications {
    public static Specification<Job> salaryBetter(final int min, final int max) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("salary"), min,max);
    }
    public static Specification<Job> inCity(final List<String> cities) {
        return (root, query, criteriaBuilder) ->root.get("address").get("city").in(cities);
    }
    public static Specification<Job> inTime(final List<String> times) {
        return (root, query, criteriaBuilder) -> root.get("time").in(times);
    }
}


