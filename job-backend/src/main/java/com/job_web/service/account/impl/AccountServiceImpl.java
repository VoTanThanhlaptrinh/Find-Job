package com.job_web.service.account.impl;

import com.job_web.data.UserRepository;
import com.job_web.dto.message.MailMessage;
import com.job_web.dto.profile.UserInfo;
import com.job_web.exception.AppException;
import com.job_web.exception.BadRequestException;
import com.job_web.exception.UnauthorizedException;
import com.job_web.message.MessageProducer;
import com.job_web.models.User;
import com.job_web.service.account.AccountService;
import com.job_web.service.support.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final VerificationService verifyService;
    private final MessageProducer mailProducer;

    @Value("${application.service.impl.subject-oauth2}")
    private String subjectOauth;

    @Value("${application.service.impl.email-oauth2}")
    private String textOauth;

    @Override
    public UserInfo getDetailUser(User user) {
        return UserInfo.fromUser(user);
    }

    @Override
    public void changePassword(String newPassword, String oldPassword, User user) {
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new BadRequestException("auth.password.current_mismatch");
        }
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public void updateInfo(UserInfo userInfo, User user) {
        userInfo.update(user);
        userRepository.save(user);
    }

    @Override
    public boolean checkOauth2(User user) {
        if (user.isOauth2Enabled() && (user.getPassword() == null || user.getPassword().isEmpty())) {
            String random = UUID.randomUUID().toString();
            String link = String.format("http://localhost:4200/reset-pass/%s", random);

            String text = String.format(textOauth, user.getEmail(), link);
            MailMessage mailMessage = new MailMessage(user.getEmail(), subjectOauth, text);
            verifyService.add("random:" + random, user.getEmail(), 60 * 60);
            verifyService.add(random, user.getEmail(), 60 * 60);
            try {
                mailProducer.sendMail(mailMessage);
            } catch (Exception e) {
                log.trace(e.getMessage(), e);
                throw new AppException("auth.email.send_failed", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return false;
        }
        return true;
    }
}
