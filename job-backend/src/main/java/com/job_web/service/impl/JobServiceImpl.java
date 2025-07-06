package com.job_web.service.impl;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import com.job_web.data.HirerRepository;
import com.job_web.data.UserRepository;
import com.job_web.data.specification.JobSpecifications;
import com.job_web.dto.AddressJobCount;
import com.job_web.dto.JobResponse;
import com.job_web.models.Hirer;
import com.job_web.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.job_web.data.JobRepository;
import com.job_web.dto.ApiResponse;
import com.job_web.models.Job;
import com.job_web.service.JobService;

import lombok.AllArgsConstructor;
@Service
@AllArgsConstructor
public class JobServiceImpl implements JobService {
	private final JobRepository jobRepository;
	private final UserRepository userRepository;
	private final HirerRepository hirerRepository;
	@Override
	public ApiResponse<Page<Job>> getListJobNewest(int page, int amount) {
		PageRequest pageable = PageRequest.of(page, amount, Sort.by("createDate").descending());
		return new ApiResponse<Page<Job>>("success", jobRepository.findAll(pageable), 200);
	}

	@Override
	public ApiResponse<Job> getJobDetailById(String id) {
		try {
			long jobId = Long.valueOf(id);
			Optional<Job> job = jobRepository.findById(jobId);
            return job.map(value -> new ApiResponse<>("success", value, 200)).orElseGet(() -> new ApiResponse<>("Id not found", null, 404));
        } catch (NumberFormatException e) {
			return new ApiResponse<Job>("Id is not number", null, 404);
		}
	}

	@Override
	public ApiResponse<Boolean> checkExistJob(String id) {
		try {
			long jobId = Long.parseLong(id);
			Optional<Job> job = jobRepository.findById(jobId);
				return new ApiResponse<Boolean>("success", job.isPresent(), 200);
		} catch (NumberFormatException e) {
			return new ApiResponse<Boolean>("Id is not number", false, 404);
		}
	}

	@Override
	public ApiResponse<Integer> getAmount() {
		return new ApiResponse<>("success", (int) jobRepository.count(), HttpStatus.OK.value());
	}

	@Override
	public ApiResponse<List<AddressJobCount>> getAddressJobCount() {
		return new ApiResponse<>("success",jobRepository.findAddressJobCount(), HttpStatus.OK.value());
	}

	@Override
	public ApiResponse<Page<Job>> getListJobByAddress(String address, int page, int amount) {
		PageRequest pageable = PageRequest.of(page, amount, Sort.by("createDate").descending());
		 return new ApiResponse<>("success",jobRepository.findByTime(address, pageable), HttpStatus.OK.value());
	}

	@Override
	public ApiResponse<Page<Job>> filterBetterSalaryAndHasAddressAndInTimes(final int pageIndex, final int pageSize, final int min,final int max,final List<String> address,final List<String> times) {
		Specification<Job> spec = Specification.where(JobSpecifications.salaryBetter(min,max)
		.or(JobSpecifications.inAddress(address))
						.or(JobSpecifications.inTime(times))
				);
		PageRequest pageable = PageRequest.of(pageIndex, pageSize, Sort.by("createDate").descending());
		return new ApiResponse<>("success",jobRepository.findAll(spec, pageable), HttpStatus.OK.value());
	}

	@Override
	public ApiResponse<String> saveJob(Job job, Principal principal) {
		if(principal == null) {
			return new ApiResponse<>("Chưa đăng nhập", null, HttpStatus.BAD_REQUEST.value());
		}
		User user = userRepository.findByEmail(principal.getName()).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng này"));
		Hirer hirer = job.getHirer();
		if(hirer.getUser().getId() != user.getId()) {
			return new ApiResponse<>("Tài khoản giả mạo", null, HttpStatus.BAD_REQUEST.value());
		}
		hirerRepository.save(hirer);
		jobRepository.save(job);
		return new ApiResponse<>("Thành công", null, HttpStatus.CREATED.value());
	}

	@Override
	public ApiResponse<Page<JobResponse>> getHirerJobPost(int pageIndex, int pageSize, Principal principal) {
		PageRequest pageable = PageRequest.of(pageIndex, pageSize,Sort.by("create_date").descending());
		return new ApiResponse<Page<JobResponse>>("success", jobRepository.getJobPostOfHirer(principal.getName(),pageable), 200);
	}

	@Override
	public ApiResponse<Long> countHirerJobPost(Principal principal) {
		long count = jobRepository.getHirerJobCount(principal.getName());
		return new ApiResponse<>("success", count, HttpStatus.OK.value());
	}
}
