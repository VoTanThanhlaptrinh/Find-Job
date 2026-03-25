package com.job_web.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JobRawDataRecord(
        String jobDescription,
        String jobRequirement,
        String jobSkill,
        String moreDetail,
        Long addressId,
        Long hirerId,
        Long imageId
) {
    // Hàm tiện ích để nối và làm sạch HTML ngay bên trong Record
    public String getCleanedCombinedText() {
        String combined = String.join(" | ",
                jobDescription != null ? jobDescription : "",
                jobRequirement != null ? jobRequirement : "",
                jobSkill != null ? jobSkill : "",
                moreDetail != null ? moreDetail : ""
        );

        // Loại bỏ toàn bộ thẻ HTML (ví dụ: <p>, <ul>, <li>, <strong>)
        return combined.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
    }
}