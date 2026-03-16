package com.job_web.service.job.impl;

import java.io.IOException;
import java.security.Principal;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import com.job_web.data.HirerRepository;
import com.job_web.data.UserRepository;
import com.job_web.data.specification.JobSpecifications;
import com.job_web.dto.job.AddressJobCount;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.JobResponse;
import com.job_web.models.Hirer;
import com.job_web.models.User;
import com.job_web.dto.job.JobApply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.job_web.data.JobRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.models.Job;
import com.job_web.service.job.JobService;

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
			long jobId = Long.parseLong(id);
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
	public ApiResponse<String> createJob(JobDTO jobDTO, Principal principal) {
		if (principal == null) {
			return new ApiResponse<>("Bạn chưa đăng nhập", null, HttpStatus.UNAUTHORIZED.value());
		}
		Optional<User> userOpt = userRepository.findByEmail(principal.getName());
		if (userOpt.isEmpty()) {
			return new ApiResponse<>("Không tìm thấy người dùng", null, HttpStatus.NOT_FOUND.value());
		}
		User user = userOpt.get();
		Hirer hirer = hirerRepository.findByUserEmail(user.getEmail())
				.orElseGet(() -> {
					Hirer newHirer = new Hirer();
					newHirer.setUser(user);
					return newHirer;
				});
		applyHirerFromDto(hirer, jobDTO);
		Job job = new Job();
		job.setHirer(hirer);
		try {
			applyJobFromDto(job, jobDTO);
		} catch (IOException e) {
			return new ApiResponse<>("Không thể đọc file ảnh", null, HttpStatus.BAD_REQUEST.value());
		}
		hirerRepository.save(hirer);
		jobRepository.save(job);
		return new ApiResponse<>("Thành công", null, HttpStatus.CREATED.value());
	}

	@Override
	public ApiResponse<String> updateJob(long id, JobDTO jobDTO, Principal principal) {
		if (principal == null) {
			return new ApiResponse<>("Bạn chưa đăng nhập", null, HttpStatus.UNAUTHORIZED.value());
		}
		Optional<Job> jobOpt = jobRepository.findById(id);
		if (jobOpt.isEmpty()) {
			return new ApiResponse<>("Không tìm thấy công việc", null, HttpStatus.NOT_FOUND.value());
		}
		Job job = jobOpt.get();
		Hirer hirer = job.getHirer();
		if (hirer == null) {
			Optional<User> userOpt = userRepository.findByEmail(principal.getName());
			if (userOpt.isEmpty()) {
				return new ApiResponse<>("Không tìm thấy người dùng", null, HttpStatus.NOT_FOUND.value());
			}
			hirer = new Hirer();
			hirer.setUser(userOpt.get());
			job.setHirer(hirer);
		}
		applyHirerFromDto(hirer, jobDTO);
		try {
			applyJobFromDto(job, jobDTO);
		} catch (IOException e) {
			return new ApiResponse<>("Không thể đọc file ảnh", null, HttpStatus.BAD_REQUEST.value());
		}
		hirerRepository.save(hirer);
		jobRepository.save(job);
		return new ApiResponse<>("Thành công", null, HttpStatus.OK.value());
	}

	@Override
	public ApiResponse<String> deleteJob(long id, Principal principal) {
		if (principal == null) {
			return new ApiResponse<>("Bạn chưa đăng nhập", null, HttpStatus.UNAUTHORIZED.value());
		}
		Optional<Job> jobOpt = jobRepository.findById(id);
		if (jobOpt.isEmpty()) {
			return new ApiResponse<>("Không tìm thấy công việc", null, HttpStatus.NOT_FOUND.value());
		}
		jobRepository.delete(jobOpt.get());
		return new ApiResponse<>("Thành công", null, HttpStatus.OK.value());
	}

	@Override
	public ApiResponse<Page<JobResponse>> getHirerJobPost(int pageIndex, int pageSize, Principal principal) {
		PageRequest pageable = PageRequest.of(pageIndex, pageSize,Sort.by("create_date").descending());
		return new ApiResponse<>("success", jobRepository.getJobPostOfHirer(principal.getName(),pageable), 200);
	}

	@Override
	public ApiResponse<Long> countHirerJobPost(Principal principal) {
		long count = jobRepository.getHirerJobCount(principal.getName());
		return new ApiResponse<>("success", count, HttpStatus.OK.value());
	}



    @Override
    public ApiResponse<Page<JobApply>> listJobUserApplied(Pageable pageable,Principal principal) {
        return new ApiResponse<>("success",jobRepository.listJobUserApplies(principal.getName(),pageable),HttpStatus.OK.value());
    }



	private void applyJobFromDto(Job job, JobDTO jobDTO) throws IOException {
		job.setTime(jobDTO.getJobType());
		job.setDescription(jobDTO.getJobDescription());
		job.setRequireDetails(jobDTO.getJobRequirement());
		job.setSkill(jobDTO.getJobSkill());
		job.setAddress(jobDTO.getLocation());
		job.setSalary(jobDTO.getSalary());
		job.setTitle(jobDTO.getJobName());
		job.setExpiredDate(jobDTO.getDeadlineCV()
				.atStartOfDay(ZoneOffset.UTC)
				.toInstant());
		if (jobDTO.getImage() != null && !jobDTO.getImage().isEmpty()) {
			job.setLogo(jobDTO.getImage().getBytes());
		}
	}

	private void applyHirerFromDto(Hirer hirer, JobDTO jobDTO) {
		hirer.setCompanyName(jobDTO.getCompanyName());
		hirer.setDescription(jobDTO.getCompanyDescription());
		hirer.setSocialLink(jobDTO.getCompayWebsite());
	}
}



