package com.nlu.recruitment.application;

import com.nlu.recruitment.api.dto.JobCardView;
import com.nlu.recruitment.api.dto.JobDto;
import com.nlu.recruitment.api.dto.JobMatchView;
import com.nlu.recruitment.api.dto.JobDetailView;
import com.nlu.identity.domain.model.User;

import java.util.List;

public interface JobService {
    JobDetailView getJobDetailById(Long id);

    Boolean checkExistJob(Long id);

    void createJob(JobDto jobDTO, User user);

    void updateJob(Long id, JobDto jobDTO, User user);

    void deleteJob(Long id, User user);
    List<JobMatchView> matchJobs(long cvId);

    void analyzeJob(Long id, User user);
}
