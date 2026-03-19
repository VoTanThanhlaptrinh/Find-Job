package com.job_web.service.account;

import com.job_web.dto.auth.ForgotPassDTO;
import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.auth.RegistationForm;
import com.job_web.dto.auth.ResetDTO;
import com.job_web.dto.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.security.Principal;

public interface AuthService {
    ApiResponse<String> register(RegistationForm registationForm);

    ApiResponse<String> sendLinkActivate(String email);

    ApiResponse<String> activeAccount(String token);

    ApiResponse<String> login(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response);

    ApiResponse<String> refreshToken(HttpServletRequest request, HttpServletResponse response);

    ApiResponse<String> logout(HttpServletRequest request, HttpServletResponse response);

    ApiResponse<String> sendCodeForgotPassword(HttpServletRequest request, String email);

    ApiResponse<String> forgotPassword(ForgotPassDTO forgotPassDTO);

    ApiResponse<String> checkRandom(String random);

    ApiResponse<String> resetPassword(ResetDTO resetDTO);

    ApiResponse<String> checkLogin(Principal principal);
}
