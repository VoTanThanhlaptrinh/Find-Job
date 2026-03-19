package com.job_web.service.account.impl;

import com.job_web.data.UserRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.message.MailMessage;
import com.job_web.dto.profile.UserInfo;
import com.job_web.message.MailProducer;
import com.job_web.models.User;
import com.job_web.service.account.AccountService;
import com.job_web.service.support.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final VerificationService verifyService;
    private final MailProducer mailProducer;

    @Value("${application.service.impl.subject-oauth2}")
    private String subjectOauth;

    @Value("${application.service.impl.email-oauth2}")
    private String textOauth;

    @Override
    public ApiResponse<UserInfo> getDetailUser(Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("ChÆ°a Ä‘Äƒng nháº­p", null, HttpStatus.BAD_REQUEST.value());
        }
        User userLogin = userRepository.findByEmail(principal.getName()).orElseThrow(RuntimeException::new);
        UserInfo userInfo = new UserInfo();
        userInfo.toUserInfo(userLogin);
        return new ApiResponse<>("success", userInfo, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> changePassword(String newPassword, String oldPassword) {
        User userLogin = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!encoder.matches(oldPassword, userLogin.getPassword())) {
            return new ApiResponse<>("password hiá»‡n táº¡i khÃ´ng khá»›p", null, HttpStatus.BAD_REQUEST.value());
        }
        userLogin.setPassword(encoder.encode(newPassword));
        userRepository.save(userLogin);
        return new ApiResponse<>("success", null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> updateInfo(UserInfo userInfo, Principal principal) {
        Optional<User> user = userRepository.findByEmail(principal.getName());
        if (user.isEmpty()) {
            return new ApiResponse<>("KhÃ´ng tÃ¬m tháº¥y user trong há»‡ thá»‘ng", null, HttpStatus.BAD_REQUEST.value());
        }
        User userLogin = user.get();
        userInfo.update(userLogin);
        userRepository.save(userLogin);
        return new ApiResponse<>("Cáº­p nháº­t thÃ nh cÃ´ng", null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> checkOauth2(Principal principal) {
        if (principal == null) {
            return new ApiResponse<>("Báº¡n chÆ°a Ä‘Äƒng nháº­p", null, HttpStatus.UNAUTHORIZED.value());
        }
        Optional<User> user = userRepository.findByEmail(principal.getName());
        if (user.isEmpty()) {
            return new ApiResponse<>("TÃ i khoáº£n nÃ y khÃ´ng tá»“n táº¡i trong há»‡ thá»‘ng",
                    principal.getName(), HttpStatus.BAD_REQUEST.value());
        }
        if (user.get().isOauth2Enabled() && (user.get().getPassword() == null || user.get().getPassword().isEmpty())) {
            String random = UUID.randomUUID().toString();
            String link = String.format("http://localhost:4200/reset-pass/%s", random);

            String text = String.format(textOauth, principal.getName(), link);
            MailMessage mailMessage = new MailMessage(principal.getName(), subjectOauth, text);
            verifyService.add("random:" + random, principal.getName(), 60 * 60);
            verifyService.add(random, principal.getName(), 60 * 60);
            try {
                mailProducer.sendMail(mailMessage);
            } catch (Exception e) {
                log.trace(e.getMessage(), e);
                return new ApiResponse<>("Gá»­i email tháº¥t báº¡i", null, HttpStatus.INTERNAL_SERVER_ERROR.value());
            }
            return new ApiResponse<>("TÃ i khoáº£n cá»§a báº¡n chÆ°a cÃ³ máº­t kháº©u, báº¡n cáº§n xÃ¡c thá»±c Ä‘á»ƒ táº¡o má»›i",
                    principal.getName(), HttpStatus.OK.value());
        }
        return new ApiResponse<>("khÃ´ng cÃ³ váº¥n Ä‘á»", principal.getName(), 301);
    }
}
