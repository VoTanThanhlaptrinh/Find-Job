package com.job_web.recruiment.application;

import com.job_web.recruiment.api.dto.JobCardView;
import com.job_web.recruiment.api.dto.JobDTO;
import com.job_web.recruiment.api.dto.JobDetailView;
import com.job_web.identity.domain.model.User;

import java.util.List;

public interface JobService {
    JobDetailView getJobDetailById(Long id);

    Boolean checkExistJob(Long id);

    void createJob(JobDTO jobDTO, User user);

    void updateJob(Long id, JobDTO jobDTO, User user);

    void deleteJob(Long id, User user);
    List<JobCardView> matchJobs(long cvId);
}
