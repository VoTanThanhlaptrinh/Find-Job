package com.job_web.service.impl;

import com.job_web.data.CVRepository;
import com.job_web.dto.ApiResponse;
import com.job_web.dto.CVDTO;
import com.job_web.models.CV;
import com.job_web.service.CVService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;
@Service
@AllArgsConstructor
public class CVServiceImpl implements CVService {
    private final CVRepository cvRepository;
    @Override
    public ApiResponse<List<CVDTO>> getListCVOfUser(Principal principal) {
        List<CVDTO> res = cvRepository.findAllByUser(principal.getName());
        String message = res.isEmpty() ? "User chưa upload CV nào lên hết" : "success";
        return new ApiResponse<>(message,res, HttpStatus.OK.value());
    }

    @Override
    public CV findById(long id) {
        return cvRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
    }
}
