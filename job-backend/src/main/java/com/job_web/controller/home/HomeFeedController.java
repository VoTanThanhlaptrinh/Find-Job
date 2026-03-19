package com.job_web.controller.home;

import com.job_web.data.BlogRepository;
import com.job_web.data.JobRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.HomeInitView;
import com.job_web.dto.job.JobViewMapper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class HomeFeedController {
    private JobRepository jobRepository;
    private BlogRepository blogRepository;

    @GetMapping("/home/init")
    public ResponseEntity<ApiResponse<HomeInitView>> getInit() {
        PageRequest jopBySalary = PageRequest.of(0, 7, Sort.by("salary").descending());
        PageRequest topJobBy = PageRequest.of(0, 5, Sort.by("createDate").ascending());
        PageRequest blogByTime = PageRequest.of(0, 3, Sort.by("amountLike").descending());
        try {
            HomeInitView response = new HomeInitView(
                    JobViewMapper.toPagedJobCardView(jobRepository.findAll(jopBySalary)),
                    JobViewMapper.toPagedJobCardView(jobRepository.findAll(topJobBy))
            );
//          blogRepository.findAll(blogByTime);
            return ResponseEntity.ok(new ApiResponse<>("Load dữ liệu thành công", response, 200));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(e.getMessage(), null, 200));
        }
    }
}
