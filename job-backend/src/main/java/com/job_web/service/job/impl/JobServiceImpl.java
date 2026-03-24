package com.job_web.service.job.impl;

import com.job_web.data.AddressRepository;
import com.job_web.data.HirerRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.UserRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.JobDetailView;
import com.job_web.dto.job.JobViewMapper;
import com.job_web.models.Address;
import com.job_web.models.Hirer;
import com.job_web.models.Job;
import com.job_web.models.User;
import com.job_web.service.job.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final HirerRepository hirerRepository;
    private final AddressRepository addressRepository;

    @Override
    public ApiResponse<JobDetailView> getJobDetailById(String id) {
        try {
            long jobId = Long.parseLong(id);
            Optional<Job> job = jobRepository.findById(jobId);
            return job.map(value -> new ApiResponse<>("success", JobViewMapper.toJobDetailView(value), HttpStatus.OK.value()))
                    .orElseGet(() -> new ApiResponse<>("Id not found", null, HttpStatus.NOT_FOUND.value()));
        } catch (NumberFormatException e) {
            return new ApiResponse<>("Id is not number", null, HttpStatus.NOT_FOUND.value());
        }
    }

    @Override
    public ApiResponse<Boolean> checkExistJob(String id) {
        try {
            long jobId = Long.parseLong(id);
            Optional<Job> job = jobRepository.findById(jobId);
            return new ApiResponse<>("success", job.isPresent(), HttpStatus.OK.value());
        } catch (NumberFormatException e) {
            return new ApiResponse<>("Id is not number", false, HttpStatus.NOT_FOUND.value());
        }
    }

    @Override
    @Transactional
    public ApiResponse<String> createJob(JobDTO jobDTO, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Ban chua dang nhap", null, HttpStatus.UNAUTHORIZED.value());
        }

        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("Khong tim thay nguoi dung", null, HttpStatus.NOT_FOUND.value());
        }

        Optional<Hirer> hirerOpt = hirerRepository.findHirerByUserIs(userOpt.get());
        if (hirerOpt.isEmpty()) {
            return new ApiResponse<>("Account nay khong du thong tin de dang tuyen", null, HttpStatus.NOT_FOUND.value());
        }

        Optional<Address> addressOpt = addressRepository.findById(jobDTO.getAddressId());
        if (addressOpt.isEmpty()) {
            return new ApiResponse<>("Khong tim thay dia chi phu hop", null, HttpStatus.NOT_FOUND.value());
        }

        Hirer hirer = hirerOpt.get();
        Address address = addressOpt.get();
        if (!hirer.isExistAddress(address)) {
            return new ApiResponse<>("Dia chi nay khong thuoc ve tai khoan dang su dung", null, HttpStatus.NOT_FOUND.value());
        }

        Job job = jobDTO.toJob();
        job.setAddress(address);
        job.setHirer(hirer);
        jobRepository.save(job);
        return new ApiResponse<>("Thanh cong", null, HttpStatus.CREATED.value());
    }

    @Override
    @Transactional
    public ApiResponse<String> updateJob(long id, JobDTO jobDTO, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Ban chua dang nhap", null, HttpStatus.UNAUTHORIZED.value());
        }

        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            return new ApiResponse<>("Khong tim thay cong viec", null, HttpStatus.NOT_FOUND.value());
        }

        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("Khong tim thay nguoi dung", null, HttpStatus.NOT_FOUND.value());
        }

        Optional<Hirer> hirerOpt = hirerRepository.findHirerByUserIs(userOpt.get());
        if (hirerOpt.isEmpty()) {
            return new ApiResponse<>("Account nay khong du thong tin de dang tuyen", null, HttpStatus.NOT_FOUND.value());
        }

        Hirer hirer = hirerOpt.get();
        Job job = jobOpt.get();
        if (job.getHirer() == null || job.getHirer().getId() != hirer.getId()) {
            return new ApiResponse<>("Ban khong co quyen chinh sua cong viec nay", null, HttpStatus.FORBIDDEN.value());
        }

        Optional<Address> addressOpt = addressRepository.findById(jobDTO.getAddressId());
        if (addressOpt.isEmpty()) {
            return new ApiResponse<>("Khong tim thay dia chi phu hop", null, HttpStatus.NOT_FOUND.value());
        }

        Address address = addressOpt.get();
        if (!hirer.isExistAddress(address)) {
            return new ApiResponse<>("Dia chi nay khong thuoc ve tai khoan dang su dung", null, HttpStatus.NOT_FOUND.value());
        }

        jobDTO.updateJob(job);
        job.setAddress(address);
        job.setHirer(hirer);
        jobRepository.save(job);
        return new ApiResponse<>("Thanh cong", null, HttpStatus.OK.value());
    }

    @Override
    @Transactional
    public ApiResponse<String> deleteJob(long id, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Ban chua dang nhap", null, HttpStatus.UNAUTHORIZED.value());
        }

        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            return new ApiResponse<>("Khong tim thay cong viec", null, HttpStatus.NOT_FOUND.value());
        }

        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("Khong tim thay nguoi dung", null, HttpStatus.NOT_FOUND.value());
        }

        Optional<Hirer> hirerOpt = hirerRepository.findHirerByUserIs(userOpt.get());
        if (hirerOpt.isEmpty()) {
            return new ApiResponse<>("Account nay khong du thong tin de dang tuyen", null, HttpStatus.NOT_FOUND.value());
        }

        Job job = jobOpt.get();
        Hirer hirer = hirerOpt.get();
        if (job.getHirer() == null || job.getHirer().getId() != hirer.getId()) {
            return new ApiResponse<>("Ban khong co quyen xoa cong viec nay", null, HttpStatus.FORBIDDEN.value());
        }

        job.markDeleted();
        jobRepository.save(job);
        return new ApiResponse<>("Thanh cong", null, HttpStatus.OK.value());
    }
}
