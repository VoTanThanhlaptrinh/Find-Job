package com.job_web.service;

import com.job_web.models.Job;
import com.job_web.models.User;

public interface ApplyService {
	public void submit(User user, Job job);
}
