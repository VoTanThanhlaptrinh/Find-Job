package com.job_web.service;

import com.job_web.dto.ApiResponse;
import com.job_web.dto.UserInfo;

public interface AccountService {
	boolean checkPassword(String passwordInput, String passwordInstored);
	public ApiResponse<UserInfo> getDetailUser();
	public ApiResponse<String> changePassword(String newPassword, String oldPassword);
}
