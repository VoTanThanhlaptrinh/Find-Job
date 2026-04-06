package com.job_web.service.account;

import com.job_web.dto.auth.ForgotPassDTO;
import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.auth.RegistationForm;
import com.job_web.dto.auth.ResetDTO;
import com.job_web.models.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    String registerUser(RegistationForm registationForm);

    String registerHirer(RegistationForm registationForm);

    void sendLinkActivate(String email);

    void activeAccount(String token);

    String loginUser(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response);

    String loginHirer(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response);

    String refreshToken(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

    void sendCodeForgotPassword(HttpServletRequest request, String email);

    String forgotPassword(ForgotPassDTO forgotPassDTO);

    void checkRandom(String random);

    void resetPassword(ResetDTO resetDTO);

    String checkLogin(User user);
}
