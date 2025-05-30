package com.job_web.service.impl;

import java.util.Optional;

import com.job_web.data.ApplyRepository;
import com.job_web.data.CVRepository;
import com.job_web.data.UserRepository;
import com.job_web.models.Apply;
import com.job_web.models.CV;
import com.job_web.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
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
	private final ApplyRepository applyRepository;
	private final CVRepository cvRepository;
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
	public ApiResponse<String> apply(String jobId, byte[] data, String fileName) {
		Optional<Job> job = jobRepository.findById(Long.parseLong(jobId));
		if(job.isEmpty()){
			return new ApiResponse<>("không tìm thấy thông tin công việc mà bạn ứng tuyển",null, 400);
		}
		User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		CV cv = new CV();
		cv.setData(data);
		cv.setUser(user);
		cv.setFileName(fileName);
		cvRepository.save(cv);
		Apply apply = new Apply();
		apply.setUser(user);
		apply.setJob(job.get());
		apply.setCv(cv);
		applyRepository.save(apply);
		return new ApiResponse<>("Ứng tuyển thành công",null, 200);
	}

}
