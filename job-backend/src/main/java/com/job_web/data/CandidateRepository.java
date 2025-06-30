package com.job_web.data;

import org.springframework.data.repository.CrudRepository;

import com.job_web.models.Candidate;

public interface CandidateRepository extends CrudRepository<Candidate, Long> {

}
