package com.job_web.data.specification;

import com.job_web.models.Job;
import org.springframework.data.jpa.domain.Specification;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

public class JobSpecifications {
    public static Specification<Job> salaryBetter(final int min, final int max) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.between(root.get("salary"), min,max);
    }
    public static Specification<Job> inAddress(final List<String> address) {
        return (root, query, criteriaBuilder) ->root.get("address").in(address);
    }
    public static Specification<Job> inTime(final List<String> times) {
        return (root, query, criteriaBuilder) -> root.get("time").in(times);
    }
}
