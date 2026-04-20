package com.job_web.dto.admin.seeker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobSeekerRequest {
    private String fullName;
    private String email;
    private String profession;
    private String resumeUrl;
}
