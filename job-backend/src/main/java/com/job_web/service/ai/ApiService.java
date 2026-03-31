package com.job_web.service.ai;


import com.job_web.dto.ai.ResumeRequest;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.VectorizeJdRequest;

import java.util.List;

public interface ApiService {
    // 2. Tạo Vector cho CV
    void vectorizeCv(ResumeRequest request);

    // 3. Tạo Vector cho JD
    void vectorizeJd(VectorizeJdRequest request);

    // 4. Cập nhật JD
    void updateJd(JobDTO request);

    // 5. Match CV với Top 10 JD
    List<JobCardView> matchJobs(Long cvId);

    // 6. Xóa JD
    void deleteJd(Long jobId);

    // 7. Xóa CV
    void deleteCv(Long cvId);
}
