package com.job_web.controller.home;

import com.job_web.data.BlogRepository;
import com.job_web.data.JobRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.HomeInitView;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobViewMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/home")
@AllArgsConstructor
public class HomeFeedController {
    private JobRepository jobRepository;
    private BlogRepository blogRepository;

    @GetMapping("/init")
    public ResponseEntity<ApiResponse<List<JobCardView>>> getInit() {
        PageRequest pageRequest = PageRequest.of(0,10, Sort.by("createDate").descending());
        PageRequest blogByTime = PageRequest.of(0, 3, Sort.by("amountLike").descending());
        return ResponseEntity.ok(new ApiResponse<>("Load dữ liệu thành công", jobRepository.findJobs(LocalDateTime.now(), "ACTIVE",pageRequest), 200));
    }
    @GetMapping()
    public ResponseEntity<String> home(){
        return ResponseEntity.ok("Hello world");
    }
}
