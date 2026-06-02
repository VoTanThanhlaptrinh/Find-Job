package com.job_web.recruiment.domain.repository;

import com.job_web.identity.domain.model.User;
import org.springframework.data.repository.CrudRepository;

import com.job_web.recruiment.domain.model.Recruitment;

import java.util.Optional;

public interface RecruitmentRepository extends CrudRepository<Recruitment, Long> {
    Optional<Recruitment> findHirerByUserIs(User user);

}


