package com.job_web.service.account.impl;

import com.job_web.data.UserRepository;
import com.job_web.dto.message.MailMessage;
import com.job_web.dto.profile.UserInfo;
import com.job_web.exception.AppException;
import com.job_web.exception.BadRequestException;
import com.job_web.message.MessageProducer;
import com.job_web.models.User;
import com.job_web.service.account.AccountService;
import com.job_web.service.support.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
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

    private static final String MDC_USER_ID = "userId";

    @Value("${application.service.impl.subject-oauth2}")
    private String subjectOauth;

    @Value("${application.service.impl.email-oauth2}")
    private String textOauth;

    @Override
    public UserInfo getDetailUser(User user) {
        // Read-only query — no MDC / no logging needed.
        // RequestLoggingFilter already covers the HTTP layer.
        return UserInfo.fromUser(user);
    }

    @Override
    public void changePassword(String newPassword, String oldPassword, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            log.info("Password change requested for user: {}", user.getId());

            if (!encoder.matches(oldPassword, user.getPassword())) {
                log.warn("Password change failed — current password mismatch for user: {}", user.getId());
                throw new BadRequestException("auth.password.current_mismatch");
            }

            user.setPassword(encoder.encode(newPassword));
            userRepository.save(user);

            log.info("Password changed successfully for user: {}", user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public void updateInfo(UserInfo userInfo, User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            log.info("Profile update requested for user: {}", user.getId());

            userInfo.update(user);
            userRepository.save(user);

            log.info("Profile updated successfully for user: {}", user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public boolean checkOauth2(User user) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            if (user.isOauth2Enabled() && (user.getPassword() == null || user.getPassword().isEmpty())) {
                log.info("OAuth2 user detected without password — initiating password setup for user: {}", user.getId());

                String random = UUID.randomUUID().toString();
                String link = String.format("http://localhost:4200/reset-pass/%s", random);

                String text = String.format(textOauth, user.getEmail(), link);
                MailMessage mailMessage = new MailMessage(user.getEmail(), subjectOauth, text);
                verifyService.add("random:" + random, user.getEmail(), 60 * 60);
                verifyService.add(random, user.getEmail(), 60 * 60);

                try {
                    mailProducer.sendMail(mailMessage);
                    log.info("OAuth2 password setup email dispatched for user: {}", user.getId());
                } catch (Exception e) {
                    log.warn("Failed to send OAuth2 password setup email for user: {}", user.getId());
                    throw new AppException("auth.email.send_failed", HttpStatus.INTERNAL_SERVER_ERROR);
                }
                return false;
            }
            return true;
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }
}
