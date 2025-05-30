package com.job_web.data;

import com.job_web.models.CV;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CVRepository extends CrudRepository<CV, Long> {
}
