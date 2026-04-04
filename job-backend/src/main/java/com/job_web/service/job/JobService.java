package com.job_web.service.job;

import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.JobDetailView;
import com.job_web.models.User;

public interface JobService {
    JobDetailView getJobDetailById(Long id);

    Boolean checkExistJob(Long id);

    void createJob(JobDTO jobDTO, User user);

    void updateJob(Long id, JobDTO jobDTO, User user);

    void deleteJob(Long id, User user);

}
