package com.job_web.dto.admin.seeker;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegionDistributionResponse {
    private List<RegionCount> regions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegionCount {
        private String code;
        private long count;
    }
}
