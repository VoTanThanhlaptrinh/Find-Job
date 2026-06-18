package com.nlu.recruitment.domain.repository;

import com.nlu.identity.domain.model.User;
import org.springframework.data.repository.CrudRepository;

import com.nlu.recruitment.domain.model.Recruitment;

import java.util.Optional;

public interface RecruitmentRepository extends CrudRepository<Recruitment, Long> {
    Optional<Recruitment> findRecruitmentByUser(User user);

}


