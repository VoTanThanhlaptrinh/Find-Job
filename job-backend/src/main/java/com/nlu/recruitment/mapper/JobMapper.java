package com.nlu.recruitment.mapper;

import com.nlu.recruitment.api.dto.JobDto;
import com.nlu.recruitment.domain.model.Job;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public void updateJob(JobDto jobDTO, Job job) {
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

    public Job toJob(JobDto jobDTO) {
        Job job = new Job();
        updateJob(jobDTO, job);
        return job;
    }
}
