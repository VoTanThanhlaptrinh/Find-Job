package com.job_web.service.admin.impl;

import com.job_web.service.admin.AdminNavigationService;
import org.springframework.stereotype.Service;

@Service
public class AdminNavigationServiceImpl implements AdminNavigationService {

    @Override
    public String dashboard() {
        return "admin dashboard";
    }

    @Override
    public String employers() {
        return "admin employers";
    }

    @Override
    public String jobSeekers() {
        return "admin job-seekers";
    }

    @Override
    public String jobs() {
        return "admin jobs";
    }

    @Override
    public String billing() {
        return "admin billing";
    }
}
