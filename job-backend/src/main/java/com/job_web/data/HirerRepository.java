package com.job_web.data;

import com.job_web.models.User;
import org.springframework.data.repository.CrudRepository;

import com.job_web.models.Hirer;

import java.util.Optional;

public interface HirerRepository extends CrudRepository<Hirer, Long> {
    Optional<Hirer> findHirerByUserIs(User user);

}


