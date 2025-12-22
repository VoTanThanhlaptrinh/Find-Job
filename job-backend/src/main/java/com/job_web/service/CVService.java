package com.job_web.service;

import com.job_web.dto.ApiResponse;
import com.job_web.dto.CVDTO;
import com.job_web.models.CV;

import java.security.Principal;
import java.util.List;

public interface CVService {
    public ApiResponse<List<CVDTO>> getListCVOfUser(Principal principal);

    public CV findById(long id);
}
