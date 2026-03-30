package com.job_web.service.ai.impl;

import com.job_web.data.queryDSL.JobQuery;
import com.job_web.dto.ai.ResumeRequest;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.job.JobCardView;
import com.job_web.dto.job.JobDTO;
import com.job_web.dto.job.MatchJobsResponse;
import com.job_web.dto.job.VectorizeJdRequest;
import com.job_web.service.ai.ApiService;
import com.job_web.service.job.JobQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.client.RestClientBuilderConfigurer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;

@Service
public class ApiServiceImpl implements ApiService {
    private final RestClient restClient;
    @Autowired
    private JobQuery jobQuery;
    public ApiServiceImpl(RestClient.Builder restClientBuilder,@Value("${spring.ai.agent.base-url}") String baseUrl){
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }
    @Override
    public void vectorizeCv(ResumeRequest request) {
        restClient.post()
                .uri("/vectorize-resume/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity(); // toBodilessEntity() vì ta không cần quan tâm body trả về
    }

    @Override
    public void vectorizeJd(VectorizeJdRequest request) {
        restClient.post()
                .uri("/vectorize-jd/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }


    @Override
    public List<JobCardView> matchJobs(Long cvId) {

        var res = restClient.get()
                .uri("/match-jobs/"+cvId)
                .retrieve()
                .toEntity(MatchJobsResponse.class);
        if(res.getBody() == null){
            return List.of();
        }
        return jobQuery.findMatchJobs(res.getBody());
    }

    @Override
    public void updateJd(JobDTO request) {
        restClient.put()
                .uri("/update-jd/")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }



    @Override
    public void deleteJd(Long jobId) {
        restClient.delete()
                .uri("/delete-jd/{jobId}", jobId)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void deleteCv(Long cvId) {
        restClient.delete()
                .uri("/delete-resume/{cvId}", cvId)
                .retrieve()
                .toBodilessEntity();
    }
}
