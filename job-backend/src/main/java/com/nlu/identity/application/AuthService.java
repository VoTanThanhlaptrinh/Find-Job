package com.nlu.identity.application;

import com.nlu.identity.api.dto.ForgotPassDTO;
import com.nlu.identity.api.dto.LoginDTO;
import com.nlu.identity.api.dto.RegistrationForm;
import com.nlu.identity.api.dto.ResetDTO;
import com.nlu.identity.domain.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    String registerUser(RegistrationForm registrationForm);

    String registerHirer(RegistrationForm registrationForm);

    void sendLinkActivate(String email);

    void activeAccount(String token);

    String loginUser(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response);

    String loginHirer(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response);
    String loginAdmin(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response);

    String refreshToken(HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

    void sendCodeForgotPassword(HttpServletRequest request, String email);

    String forgotPassword(ForgotPassDTO forgotPassDTO);

    void checkRandom(String random);

    void resetPassword(ResetDTO resetDTO);

    String checkLogin(User user);
}
