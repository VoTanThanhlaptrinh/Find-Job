package com.job_web.service.job.impl;

import com.job_web.data.AddressRepository;
import com.job_web.data.HirerRepository;
import com.job_web.data.JobRepository;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.JobDetailView;
import com.job_web.dto.job.JobViewMapper;
import com.job_web.exception.ForbiddenException;
import com.job_web.exception.ResourceNotFoundException;
import com.job_web.models.Address;
import com.job_web.models.Hirer;
import com.job_web.models.Job;
import com.job_web.models.User;
import com.job_web.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final HirerRepository hirerRepository;
    private final AddressRepository addressRepository;

    @Override
    public JobDetailView getJobDetailById(Long id) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("job.not_found"));
        return JobViewMapper.toJobDetailView(job);
    }

    @Override
    public Boolean checkExistJob(Long id) {
        return jobRepository.findById(id).isPresent();
    }

    @Override
    @Transactional
    public void createJob(JobDTO jobDTO, User user) {
        Hirer hirer = hirerRepository.findHirerByUserIs(user)
                .orElseThrow(() -> new ForbiddenException("message.forbidden"));

        Address address = addressRepository.findById(jobDTO.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("message.not_found"));

        if (!hirer.isExistAddress(address)) {
            throw new ForbiddenException("job.address.forbidden");
        }

        Job job = jobDTO.toJob();
        job.setAddress(address);
        job.setHirer(hirer);
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void updateJob(Long id, JobDTO jobDTO, User user) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("job.not_found"));

        Hirer hirer = hirerRepository.findHirerByUserIs(user)
                .orElseThrow(() -> new ForbiddenException("message.forbidden"));

        if (job.getHirer() == null || job.getHirer().getId() != hirer.getId()) {
            throw new ForbiddenException("job.edit.forbidden");
        }

        Address address = addressRepository.findById(jobDTO.getAddressId())
                .orElseThrow(() -> new ResourceNotFoundException("message.not_found"));

        if (!hirer.isExistAddress(address)) {
            throw new ForbiddenException("job.address.forbidden");
        }

        jobDTO.updateJob(job);
        job.setAddress(address);
        job.setHirer(hirer);
        jobRepository.save(job);
    }

    @Override
    @Transactional
    public void deleteJob(Long id, User user) {
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("job.not_found"));

        Hirer hirer = hirerRepository.findHirerByUserIs(user)
                .orElseThrow(() -> new ForbiddenException("message.forbidden"));

        if (job.getHirer() == null || job.getHirer().getId() != hirer.getId()) {
            throw new ForbiddenException("job.delete.forbidden");
        }

        job.markDeleted();
        jobRepository.save(job);
    }
}
