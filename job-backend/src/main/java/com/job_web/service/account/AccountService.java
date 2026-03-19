package com.job_web.service.account;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.profile.UserInfo;

import java.security.Principal;

public interface AccountService {
	ApiResponse<UserInfo> getDetailUser(Principal principal);
	ApiResponse<String> changePassword(String newPassword, String oldPassword);
	ApiResponse<String> updateInfo( UserInfo userInfo, Principal principal);

	ApiResponse<String> checkOauth2(Principal principal);
}



