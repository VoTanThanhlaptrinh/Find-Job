package com.nlu.recruitment.application.impl;

import com.nlu.recruitment.domain.vo.EmploymentType;
import com.nlu.recruitment.domain.repository.AddressRepository;
import com.nlu.recruitment.domain.repository.RecruitmentRepository;
import com.nlu.recruitment.domain.repository.JobRepository;
import com.nlu.recruitment.api.dto.JobCardView;
import com.nlu.recruitment.api.dto.JobDto;
import com.nlu.recruitment.api.dto.JobDetailView;
import com.nlu.recruitment.mapper.JobViewMapper;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.recruitment.mapper.JobMapper;
import com.nlu.recruitment.domain.model.Address;
import com.nlu.recruitment.domain.model.Recruitment;
import com.nlu.recruitment.domain.model.Job;
import com.nlu.identity.domain.model.User;
import com.nlu.recruitment.application.JobService;
import jakarta.persistence.Tuple;
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
    private final RecruitmentRepository recruitmentRepository;
    private final AddressRepository addressRepository;
    private final DefaultRepositoryTagsProvider repositoryTagsProvider;
    private final JobMapper jobMapper;
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_JOB_ID = "jobId";
    private final JobViewMapper jobViewMapper;
    @Override
    public JobDetailView getJobDetailById(Long id) {
        // Read-only — no logging needed.
        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("job.not_found"));
        return jobViewMapper.toJobDetailView(job);
    }

    @Override
    public Boolean checkExistJob(Long id) {
        return jobRepository.findById(id).isPresent();
    }

    @Override
    @Transactional
    public void createJob(JobDto jobDTO, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            log.info("Creating job post — title: {}, hirer user: {}", jobDTO.getJobName(), user.getId());

            Recruitment recruitment = recruitmentRepository.findHirerByUserIs(user)
                    .orElseThrow(() -> new ForbiddenException("message.forbidden"));

            Address address = addressRepository.findById(jobDTO.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("message.not_found"));

            if (!recruitment.isExistAddress(address)) {
                log.warn("Job creation forbidden — hirer: {} does not own address: {}",
                        recruitment.getId(), jobDTO.getAddressId());
                throw new ForbiddenException("job.address.forbidden");
            }

            Job job = jobMapper.toJob(jobDTO);
            job.setAddress(address);
            job.setRecruitment(recruitment);
            jobRepository.save(job);

            MDC.put(MDC_JOB_ID, String.valueOf(job.getId()));
            log.info("Job post created — job: {}, title: {}, by hirer: {}",
                    job.getId(), job.getTitle(), recruitment.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
            MDC.remove(MDC_JOB_ID);
        }
    }

    @Override
    @Transactional
    public void updateJob(Long id, JobDto jobDTO, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));
            MDC.put(MDC_JOB_ID, String.valueOf(id));

            log.info("Updating job post: {} by user: {}", id, user.getId());

            Job job = jobRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("job.not_found"));

            Recruitment recruitment = recruitmentRepository.findHirerByUserIs(user)
                    .orElseThrow(() -> new ForbiddenException("message.forbidden"));

            if (job.getRecruitment() == null || job.getRecruitment().getId() != recruitment.getId()) {
                log.warn("Job update forbidden — user: {} is not owner of job: {}", user.getId(), id);
                throw new ForbiddenException("job.edit.forbidden");
            }

            Address address = addressRepository.findById(jobDTO.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("message.not_found"));

            if (!recruitment.isExistAddress(address)) {
                log.warn("Job update forbidden — hirer: {} does not own address: {}",
                        recruitment.getId(), jobDTO.getAddressId());
                throw new ForbiddenException("job.address.forbidden");
            }

            jobMapper.updateJob(jobDTO,job);
            job.setAddress(address);
            job.setRecruitment(recruitment);
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

            Recruitment recruitment = recruitmentRepository.findHirerByUserIs(user)
                    .orElseThrow(() -> new ForbiddenException("message.forbidden"));

            if (job.getRecruitment() == null || job.getRecruitment().getId() != recruitment.getId()) {
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
        List<Tuple> tuples = jobRepository.matchJobs(cvId);
        return tuples.stream().map(t -> {
            String timeStr = t.get("time", String.class);

            EmploymentType employmentType = null;
            if (timeStr != null && !timeStr.trim().isEmpty()) {
                try {
                    employmentType = EmploymentType.valueOf(timeStr.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    System.err.println("Unrecognized EmploymentType in DB: " + timeStr);
                }
            }
            return new JobCardView(
                    t.get("id", Number.class).longValue(),
                    t.get("title", String.class),
                    t.get("address", String.class),
                    t.get("salary", String.class),
                    employmentType
            );
        }).toList();
    }
}
