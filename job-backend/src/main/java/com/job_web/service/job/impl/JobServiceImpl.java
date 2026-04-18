package com.job_web.service.job.impl;

import com.job_web.data.AddressRepository;
import com.job_web.data.HirerRepository;
import com.job_web.data.JobRepository;
import com.job_web.dto.job.JobCardView;
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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.actuate.metrics.data.DefaultRepositoryTagsProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final HirerRepository hirerRepository;
    private final AddressRepository addressRepository;
    private final DefaultRepositoryTagsProvider repositoryTagsProvider;

    private static final String MDC_USER_ID = "userId";
    private static final String MDC_JOB_ID = "jobId";

    @Override
    public JobDetailView getJobDetailById(Long id) {
        // Read-only — no logging needed.
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("job.not_found"));
        return JobViewMapper.toJobDetailView(job);
    }

    @Override
    public Boolean checkExistJob(Long id) {
        // Read-only — no logging needed.
        return jobRepository.findById(id).isPresent();
    }

    @Override
    @Transactional
    public void createJob(JobDTO jobDTO, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            log.info("Creating job post — title: {}, hirer user: {}", jobDTO.getJobName(), user.getId());

            Hirer hirer = hirerRepository.findHirerByUserIs(user)
                    .orElseThrow(() -> new ForbiddenException("message.forbidden"));

            Address address = addressRepository.findById(jobDTO.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("message.not_found"));

            if (!hirer.isExistAddress(address)) {
                log.warn("Job creation forbidden — hirer: {} does not own address: {}",
                        hirer.getId(), jobDTO.getAddressId());
                throw new ForbiddenException("job.address.forbidden");
            }

            Job job = jobDTO.toJob();
            job.setAddress(address);
            job.setHirer(hirer);
            jobRepository.save(job);

            MDC.put(MDC_JOB_ID, String.valueOf(job.getId()));
            log.info("Job post created — job: {}, title: {}, by hirer: {}",
                    job.getId(), job.getTitle(), hirer.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_JOB_ID);
        }
    }

    @Override
    @Transactional
    public void updateJob(Long id, JobDTO jobDTO, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_JOB_ID, String.valueOf(id));

            log.info("Updating job post: {} by user: {}", id, user.getId());

            Job job = jobRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("job.not_found"));

            Hirer hirer = hirerRepository.findHirerByUserIs(user)
                    .orElseThrow(() -> new ForbiddenException("message.forbidden"));

            if (job.getHirer() == null || job.getHirer().getId() != hirer.getId()) {
                log.warn("Job update forbidden — user: {} is not owner of job: {}", user.getId(), id);
                throw new ForbiddenException("job.edit.forbidden");
            }

            Address address = addressRepository.findById(jobDTO.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("message.not_found"));

            if (!hirer.isExistAddress(address)) {
                log.warn("Job update forbidden — hirer: {} does not own address: {}",
                        hirer.getId(), jobDTO.getAddressId());
                throw new ForbiddenException("job.address.forbidden");
            }

            jobDTO.updateJob(job);
            job.setAddress(address);
            job.setHirer(hirer);
            jobRepository.save(job);

            log.info("Job post updated — job: {}, title: {}", id, job.getTitle());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_JOB_ID);
        }
    }

    @Override
    @Transactional
    public void deleteJob(Long id, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_JOB_ID, String.valueOf(id));

            Job job = jobRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("job.not_found"));

            Hirer hirer = hirerRepository.findHirerByUserIs(user)
                    .orElseThrow(() -> new ForbiddenException("message.forbidden"));

            if (job.getHirer() == null || job.getHirer().getId() != hirer.getId()) {
                log.warn("Job deletion forbidden — user: {} is not owner of job: {}", user.getId(), id);
                throw new ForbiddenException("job.delete.forbidden");
            }

            job.markDeleted();
            jobRepository.save(job);

            log.info("Job post soft-deleted — job: {} by user: {}", id, user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_JOB_ID);
        }
    }

    @Override
    public List<JobCardView> matchJobs(long cvId) {
        // Read-only — no logging needed.
        return jobRepository.matchJobs(cvId);
    }
}
