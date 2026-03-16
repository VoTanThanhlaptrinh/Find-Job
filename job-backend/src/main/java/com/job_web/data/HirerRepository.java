package com.job_web.data;

import org.springframework.data.repository.CrudRepository;

import com.job_web.models.Hirer;

import java.util.Optional;

public interface HirerRepository extends CrudRepository<Hirer, Long> {
    Optional<Hirer> findByUserEmail(String email);

}


