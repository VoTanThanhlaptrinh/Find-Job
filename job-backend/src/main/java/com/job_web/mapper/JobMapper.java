package com.job_web.mapper;

import com.job_web.dto.job.JobDTO;
import com.job_web.models.Job;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public void updateJob(JobDTO jobDTO, Job job) {
        job.setTime(jobDTO.jobType());
        job.setDescription(jobDTO.jobDescription());
        job.setRequireDetails(jobDTO.jobRequirement());
        job.setSalary(jobDTO.salary());
        job.setTitle(jobDTO.jobName());
        if(jobDTO.moreDetail() != null && !jobDTO.moreDetail().isEmpty()){
            job.setMoreDetail(jobDTO.moreDetail());
        }
        if(jobDTO.headcount() != null && jobDTO.headcount() > 0){
            job.setHeadcount(jobDTO.headcount());
        }
    }

    public Job toJob(JobDTO jobDTO) {
        Job job = new Job();
        updateJob(jobDTO, job);
        return job;
    }
}
