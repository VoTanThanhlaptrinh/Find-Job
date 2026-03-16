package com.job_web.controller.home;

import java.util.HashMap;
import java.util.Map;

import com.job_web.data.BlogRepository;
import com.job_web.data.JobRepository;
import com.job_web.dto.common.ApiResponse;
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
    public ResponseEntity<ApiResponse<Object>> getInit() {
        Map<String, Object> response = new HashMap<>();
        PageRequest jopBySalary = PageRequest.of(0, 7, Sort.by("salary").descending());
        PageRequest topJobBy = PageRequest.of(0, 5, Sort.by("createDate").ascending());
        PageRequest blogByTime = PageRequest.of(0, 3, Sort.by("amountLike").descending());
        try {
            response.put("jobSalary", jobRepository.findAll(jopBySalary));
            response.put("jobSoon", jobRepository.findAll(topJobBy));
//          response.put("blog", blogRepository.findAll(blogByTime));
            return ResponseEntity.ok(new ApiResponse<>("Load dá»¯ liá»‡u thÃ nh cÃ´ng", response, 200));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ApiResponse<>(e.getMessage(), null, 200));
        }
    }
}
