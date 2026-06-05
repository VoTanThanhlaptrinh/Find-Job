package com.nlu.recruitment.api;

import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.recruitment.api.dto.JobCardView;
import com.nlu.recruitment.infrastructure.cache.CategoryCacheService;
import com.nlu.shared.utils.MessageUtils;
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
    private final CategoryCacheService categoryCacheService;

    @GetMapping("/init")
    public ResponseEntity<ApiResponse<List<JobCardView>>> getInit() {
        List<JobCardView> data = categoryCacheService.getHomeInitData();
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("job.load.success"), data, 200));
    }

    @GetMapping()
    public ResponseEntity<String> home() {
        return ResponseEntity.ok("Hello world");
    }
}
