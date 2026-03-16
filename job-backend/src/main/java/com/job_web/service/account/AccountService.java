package com.job_web.service.account;

import com.job_web.dto.auth.ForgotPassDTO;
import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.auth.RegistationForm;
import com.job_web.dto.auth.ResetDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.profile.UserInfo;
import com.job_web.models.Job;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


import java.security.Principal;
import java.util.List;

public interface AccountService {
	boolean checkPassword(String passwordInput, String passwordInstored);
	ApiResponse<UserInfo> getDetailUser(Principal principal);
	ApiResponse<String> changePassword(String newPassword, String oldPassword);
	ApiResponse<String> resetPassword(ResetDTO resetDTO);
	ApiResponse<String> sendCodeForgotPassword(HttpServletRequest request, String email);
	ApiResponse<String> register(RegistationForm registationForm);
	ApiResponse<String> sendLinkActivate(String email);
	ApiResponse<String> activeAccount(String token);
	ApiResponse<String> login(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response);
	ApiResponse<String> refreshToken(HttpServletRequest request);
	ApiResponse<String> logout(HttpServletRequest request, HttpServletResponse response);
	ApiResponse<String> forgotPassword(ForgotPassDTO forgotPassDTO);
	ApiResponse<String> checkRandom(String random);

	ApiResponse<String> updateInfo( UserInfo userInfo, Principal principal);

	ApiResponse<String> checkOauth2(Principal principal);
    ApiResponse<List<Job>> listJobUserApplied(Principal principal);
}



