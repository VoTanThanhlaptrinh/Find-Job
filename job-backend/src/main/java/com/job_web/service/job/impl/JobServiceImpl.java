package com.job_web.service.job.impl;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import com.job_web.data.AddressRepository;
import com.job_web.data.HirerRepository;
import com.job_web.data.JobRepository;
import com.job_web.data.UserRepository;
import com.job_web.data.specification.JobSpecifications;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.HirerJobPostView;
import com.job_web.dto.job.JobApply;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.JobDetailView;
import com.job_web.dto.job.JobResponse;
import com.job_web.dto.job.JobViewMapper;
import com.job_web.dto.job.PagedPayload;
import com.job_web.models.Address;
import com.job_web.models.Hirer;
import com.job_web.models.Job;
import com.job_web.models.User;
import com.job_web.service.job.JobService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class JobServiceImpl implements JobService {
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final HirerRepository hirerRepository;
    private final AddressRepository addressRepository;

    @Override
    public ApiResponse<PagedPayload<JobCardView>> getListJobNewest(int page, int amount) {
        PageRequest pageable = PageRequest.of(page, amount, Sort.by("createDate").descending());
        return new ApiResponse<>("success", JobViewMapper.toPagedJobCardView(jobRepository.findAll(pageable)), 200);
    }

    @Override
    public ApiResponse<JobDetailView> getJobDetailById(String id) {
        try {
            long jobId = Long.parseLong(id);
            Optional<Job> job = jobRepository.findById(jobId);
            return job.map(value -> new ApiResponse<>("success", JobViewMapper.toJobDetailView(value), 200))
                    .orElseGet(() -> new ApiResponse<>("Id not found", null, 404));
        } catch (NumberFormatException e) {
            return new ApiResponse<>("Id is not number", null, 404);
        }
    }

    @Override
    public ApiResponse<Boolean> checkExistJob(String id) {
        try {
            long jobId = Long.parseLong(id);
            Optional<Job> job = jobRepository.findById(jobId);
            return new ApiResponse<>("success", job.isPresent(), 200);
        } catch (NumberFormatException e) {
            return new ApiResponse<>("Id is not number", false, 404);
        }
    }

    @Override
    public ApiResponse<Integer> getAmount() {
        return new ApiResponse<>("success", (int) jobRepository.count(), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<List<AddressJobCount>> getAddressJobCount() {
        return new ApiResponse<>("success", jobRepository.findAddressJobCount(), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<PagedPayload<JobCardView>> getListJobByAddress(String address, int page, int amount) {
        PageRequest pageable = PageRequest.of(page, amount, Sort.by("createDate").descending());
        return new ApiResponse<>("success", JobViewMapper.toPagedJobCardView(jobRepository.findByTime(address, pageable)), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<PagedPayload<JobCardView>> filterBetterSalaryAndHasAddressAndInTimes(final int pageIndex, final int pageSize, final int min, final int max, final List<String> address, final List<String> times) {
        Specification<Job> spec = Specification.where(JobSpecifications.salaryBetter(min, max)
                .or(JobSpecifications.inAddress(address))
                .or(JobSpecifications.inTime(times))
        );
        PageRequest pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createDate").descending());
        return new ApiResponse<>("success", JobViewMapper.toPagedJobCardView(jobRepository.findAll(spec, pageable)), HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> createJob(JobDTO jobDTO, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Ban chua dang nhap", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<User> userOpt = userRepository.findByEmail(principal.getName());
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("Khong tim thay nguoi dung", null, HttpStatus.NOT_FOUND.value());
        }

        User user = userOpt.get();
        Optional<Hirer> hirerOpt = hirerRepository.findHirerByUserIs(user);
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

        User user = userOpt.get();
        Optional<Hirer> hirerOpt = hirerRepository.findHirerByUserIs(user);
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
    public ApiResponse<String> deleteJob(long id, Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Báº¡n chÆ°a Ä‘Äƒng nháº­p", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<Job> jobOpt = jobRepository.findById(id);
        if (jobOpt.isEmpty()) {
            return new ApiResponse<>("KhÃ´ng tÃ¬m tháº¥y cÃ´ng viá»‡c", null, HttpStatus.NOT_FOUND.value());
        }
        Job job = jobOpt.get();
        job.markDeleted();
        jobRepository.save(job);
        return new ApiResponse<>("ThÃ nh cÃ´ng", null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<PagedPayload<HirerJobPostView>> getHirerJobPost(int pageIndex, int pageSize, Principal principal) {
        PageRequest pageable = PageRequest.of(pageIndex, pageSize, Sort.by("create_date").descending());
        Page<JobResponse> page = jobRepository.getJobPostOfHirer(principal.getName(), pageable);
        return new ApiResponse<>("success", new PagedPayload<>(page.map(JobViewMapper::toHirerJobPostView).getContent()), 200);
    }

    @Override
    public ApiResponse<Long> countHirerJobPost(Principal principal) {
        long count = jobRepository.getHirerJobCount(principal.getName());
        return new ApiResponse<>("success", count, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<Page<JobApply>> listJobUserApplied(Pageable pageable, Principal principal) {
        return new ApiResponse<>("success", jobRepository.listJobUserApplies(principal.getName(), pageable), HttpStatus.OK.value());
    }
}
