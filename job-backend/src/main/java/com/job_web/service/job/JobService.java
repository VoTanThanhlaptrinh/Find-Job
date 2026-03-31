package com.job_web.service.job;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.JobDetailView;

import java.security.Principal;

public interface JobService {
    ApiResponse<JobDetailView> getJobDetailById(String id);

    ApiResponse<Boolean> checkExistJob(String id);

    ApiResponse<String> createJob(JobDTO jobDTO, Principal principal);

    ApiResponse<String> updateJob(long id, JobDTO jobDTO, Principal principal);

    ApiResponse<String> deleteJob(long id, Principal principal);

}



