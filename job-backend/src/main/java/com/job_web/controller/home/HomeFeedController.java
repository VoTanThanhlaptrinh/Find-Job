package com.job_web.controller.home;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobCardView;
import com.job_web.service.cache.HomeCategoryCacheService;
import com.job_web.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeFeedController {
    private final HomeCategoryCacheService homeCategoryCacheService;

    @GetMapping("/init")
    public ResponseEntity<ApiResponse<List<JobCardView>>> getInit() {
        List<JobCardView> data = homeCategoryCacheService.getHomeInitData();
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("job.load.success"), data, 200));
    }

    @GetMapping()
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("Hello world");
    }
}
