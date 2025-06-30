package com.job_web.service;

import com.job_web.dto.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;

public interface AccountService {
	boolean checkPassword(String passwordInput, String passwordInstored);
	ApiResponse<UserInfo> getDetailUser(Principal principal);
	ApiResponse<String> changePassword(String newPassword, String oldPassword);
	ApiResponse<String> resetPassword(ResetDTO resetDTO);
	ApiResponse<String> sendCodeForgotPassword(HttpServletRequest request,String email);
	ApiResponse<String> register(RegistationForm registationForm);
	ApiResponse<String> sendLinkActivate(String email);
	ApiResponse<String> activeAccount(String token);
	ApiResponse<String> login(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response);
	ApiResponse<String> refreshToken(HttpServletRequest request);
	ApiResponse<String> logout(HttpServletRequest request, HttpServletResponse response);
	ApiResponse<String> forgotPassword(ForgotPassDTO forgotPassDTO);
	ApiResponse<String> checkRandom(String random);
}
