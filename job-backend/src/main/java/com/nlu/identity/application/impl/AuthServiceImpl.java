package com.nlu.identity.application.impl;

import com.nlu.identity.domain.vo.Password;
import com.nlu.identity.domain.vo.RoleConstants;
import com.nlu.recruitment.domain.repository.RecruitmentRepository;
import com.nlu.identity.domain.repository.UserRepository;
import com.nlu.recruitment.domain.vo.SocialLink;
import com.nlu.identity.api.dto.ForgotPassDTO;
import com.nlu.identity.api.dto.LoginDTO;
import com.nlu.identity.api.dto.RegistrationForm;
import com.nlu.identity.api.dto.ResetDTO;
import com.nlu.shared.api.message.dto.MailMessage;
import com.nlu.shared.domain.exception.AppException;
import com.nlu.shared.domain.exception.BadRequestException;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.domain.exception.UnauthorizedException;
import com.nlu.identity.mapper.RegistrationFormMapper;
import com.nlu.shared.infrastructure.message.MessageProducer;
import com.nlu.recruitment.domain.model.Recruitment;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.application.AuthService;
import com.nlu.identity.application.JwtFamilyService;
import com.nlu.identity.application.JwtService;
import com.nlu.identity.application.RefreshTokenService;
import com.nlu.identity.application.UserDetailsCacheService;
import com.nlu.shared.infrastructure.cache.HotCacheService;
import com.nlu.shared.application.ReferenceService;
import com.nlu.shared.application.SpamService;
import com.nlu.shared.application.VerificationService;
import com.nlu.shared.utils.IpUtils;
import com.nlu.shared.utils.MessageUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final RecruitmentRepository recruitmentRepository;
    private final ReferenceService refService;
    private final VerificationService verifyService;
    private final MessageProducer messageProducer;
    private final RefreshTokenService refreshTokenService;
    private final JwtFamilyService jwtFamilyService;
    private final SpamService spamService;
    private final JwtService jwtService;
    private final RegistrationFormMapper mapper;
    private UserDetailsCacheService userDetailsCacheService;
    private HotCacheService hotCacheService;

    @Autowired(required = false)
    void setUserDetailsCacheService(UserDetailsCacheService userDetailsCacheService) {
        this.userDetailsCacheService = userDetailsCacheService;
    }

    @Autowired(required = false)
    void setHotCacheService(HotCacheService hotCacheService) {
        this.hotCacheService = hotCacheService;
    }
    @Value("${app.frontend-url}")
    private String url;
    private static final String MDC_USER_ID = "userId";

    @Value("${application.service.impl.timeLimit}")
    private long refreshTokenTtlSeconds;

    @Value("${app.cookie.secure}")
    private boolean isSecure;

    @Value("${application.service.impl.subject-verify}")
    private String subjectVerify;

    @Value("${application.service.impl.email-verify}")
    private String textVerify;

    @Override
    @Transactional
    public String registerUser(RegistrationForm registrationForm) {
        return registerByRole(registrationForm, RoleConstants.USER);
    }

    @Override
    @Transactional
    public String registerHirer(com.nlu.identity.api.dto.HirerRegistrationForm registrationForm) {
        log.info("Registration started for role: {}", RoleConstants.HIRER);

        User user = mapper.toUser(registrationForm, encoder, RoleConstants.HIRER);
        userRepository.saveAndFlush(user);

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            Recruitment recruitment = new Recruitment();
            recruitment.setUser(user);
            recruitment.setCompanyName(registrationForm.companyName());
            recruitment.setDescription("");
            recruitment.setSocialLink(new SocialLink(""));
            recruitment.setAddresses(new ArrayList<>());
            recruitmentRepository.save(recruitment);
            log.info("Hirer profile created for user: {}", user.getId());

            sendLinkActivate(user.getUsername());

            log.info("Registration completed for user: {} with role: {}", user.getId(), RoleConstants.HIRER);
        } finally {
            MDC.remove(MDC_USER_ID);
        }
        return user.getUsername();
    }

    @Override
    public void sendLinkActivate(String email) {
        User user = userRepository.findByEmail_Value(email)
                .orElseThrow(() -> new BadRequestException("auth.user.not_found"));

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            String link = createLink(user);
            String text = String.format(textVerify, link);
            MailMessage mailMessage = new MailMessage(email, subjectVerify, text);

            try {
                messageProducer.sendMail(mailMessage);
                log.info("Activation email dispatched for user: {}", user.getId());
            } catch (Exception e) {
                log.warn("Failed to send activation email for user: {}", user.getId());
                throw new AppException("auth.email.send_failed", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public void activeAccount(String token) {
        final String rawSubject = jwtService.extractUsername(token);
        String[] values = StringUtils.delimitedListToStringArray(rawSubject, "|");
        if (values.length == 0 || !StringUtils.hasText(values[0])) {
            throw new BadRequestException("auth.token.expired");
        }
        User user = userRepository.findByEmail_Value(values[0])
                .orElseThrow(() -> new BadRequestException("auth.user.not_found"));

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            if (!jwtService.isActivationTokenValid(token, user.getEmail())) {
                log.warn("Account activation failed — invalid or expired token for user: {}", user.getId());
                throw new BadRequestException("auth.token.expired");
            }
            user.setActive(true);
            userRepository.save(user);
            if (userDetailsCacheService != null) {
                userDetailsCacheService.evictByEmail(user.getEmail());
            }
            if (hotCacheService != null) {
                hotCacheService.evictRule("admin_users_page");
            }
            log.info("Account activated successfully for user: {}", user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public String loginUser(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
        return loginByRole(loginDTO, request, response, RoleConstants.USER);
    }

    @Override
    public String loginHirer(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
        return loginByRole(loginDTO, request, response, RoleConstants.HIRER);
    }

    @Override
    public String loginAdmin(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
        return loginByRole(loginDTO, request, response, RoleConstants.ADMIN);
    }

    private String loginByRole(LoginDTO loginDTO,
            HttpServletRequest request,
            HttpServletResponse response,
            String expectedRole) {
        long startNs = System.nanoTime();
        String ip = IpUtils.getClientIp(request);

        long spamCheckStartNs = System.nanoTime();
        boolean isSpam = spamService.checkIpSpamLogin(ip);
        long redisSpamCheckMs = (System.nanoTime() - spamCheckStartNs) / 1_000_000L;
        if (isSpam) {
            log.warn("Login rate-limited for IP: {}", ip);
            throw new AppException(spamService.getMessageLoginSpam(ip), HttpStatus.TOO_MANY_REQUESTS);
        }

        long dbStartNs = System.nanoTime();
        User user = userRepository.findByEmail_Value(loginDTO.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("auth.email.not_found"));
        long dbFindUserMs = (System.nanoTime() - dbStartNs) / 1_000_000L;

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            log.info("Login attempt for user: {} with role: {}", user.getId(), expectedRole);

            validateAccountRole(user, expectedRole);

            long bcryptStartNs = System.nanoTime();
            boolean passwordMatched = encoder.matches(loginDTO.getPassword(), user.getPassword());
            long javaBcryptMs = (System.nanoTime() - bcryptStartNs) / 1_000_000L;
            if (!passwordMatched) {
                spamService.addIpSpamLogin(ip);
                log.warn("Login failed — wrong password for user: {}", user.getId());
                throw new BadRequestException("auth.login.wrong_password");
            }

            validateAccountStatus(user);

            long delSpamStartNs = System.nanoTime();
            spamService.deleteIpSpamLogin(ip);
            long redisDeleteSpamMs = (System.nanoTime() - delSpamStartNs) / 1_000_000L;

            long accessJwtStartNs = System.nanoTime();
            String accessToken = jwtService.generateToken(user);
            long javaAccessJwtMs = (System.nanoTime() - accessJwtStartNs) / 1_000_000L;

            long refreshTokenStartNs = System.nanoTime();
            String refreshToken = refreshTokenService.createRefreshToken(loginDTO.getUsername());
            long redisRefreshTokenMs = (System.nanoTime() - refreshTokenStartNs) / 1_000_000L;

            writeRefreshCookie(response, refreshToken, refreshTokenTtlSeconds);

            long totalMs = (System.nanoTime() - startNs) / 1_000_000L;
            log.info("Login successful for user: {} with role: {}", user.getId(), expectedRole);
            log.info("[PERF][Service:DB+Redis+Java][AuthService] loginByRole userId={} | total={}ms (redisSpamCheck={}ms, dbFindUser={}ms, javaBcrypt={}ms, redisDeleteSpam={}ms, javaAccessJwt={}ms, redisRefreshToken={}ms)",
                    user.getId(), totalMs, redisSpamCheckMs, dbFindUserMs, javaBcryptMs, redisDeleteSpamMs, javaAccessJwtMs, redisRefreshTokenMs);
            return accessToken;
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public String refreshToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            throw new BadRequestException("auth.cookie.expired");
        }
        for (Cookie cookie : cookies) {
            if (!"refreshToken".equals(cookie.getName())) {
                continue;
            }
            String token = cookie.getValue();
            if (refreshTokenService.isValid(token)) {
                String username = jwtService.extractUsername(token);
                User user = userRepository.findByEmail_Value(username)
                        .orElseThrow(() -> new ResourceNotFoundException("auth.user.not_found"));
                validateAccountStatus(user);
                String accessToken = jwtService.generateToken(user);
                String refreshTokenVal = refreshTokenService.reGenerateRefreshToken(token);
                writeRefreshCookie(response, refreshTokenVal, refreshTokenTtlSeconds);

                log.info("Token refreshed successfully");
                return accessToken;
            }
            log.warn("Refresh token expired — session invalidated");
            throw new ResourceNotFoundException("auth.session.expired");
        }
        throw new ResourceNotFoundException("auth.refresh_token.not_found");
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        writeRefreshCookie(response, "", 0);
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (!"refreshToken".equals(cookie.getName())) {
                    continue;
                }
                String token = cookie.getValue();
                if (StringUtils.hasText(token)) {
                    refreshTokenService.deleteRefreshToken(token);
                }
            }
        }
        response.setHeader("Authorization", "");

        log.info("User logged out — session and refresh token cleared");
    }

    private void writeRefreshCookie(HttpServletResponse response, String tokenValue, long maxAgeSeconds) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", tokenValue)
                .httpOnly(true)
                .secure(isSecure)
                .path("/")
                .sameSite("Lax")
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public void sendCodeForgotPassword(HttpServletRequest request, String email) {
        String ip = IpUtils.getClientIp(request);
        if (email == null || email.isEmpty()) {
            throw new BadRequestException("auth.email.empty");
        }
        if (spamService.checkIpSpamEmail(ip)) {
            log.warn("Forgot-password email rate-limited for IP: {}", ip);
            throw new BadRequestException(spamService.getMessageEmailSpam(ip));
        }
        spamService.addIpSpamEmail(ip);

        User user = userRepository.findByEmail_Value(email)
                .orElseThrow(() -> new BadRequestException("auth.email.not_found"));

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            String refCode = refService.getRef(6).toUpperCase();
            String subject = MessageUtils.getMessage("auth.forgot_password.subject");
            String body = MessageUtils.getMessage("auth.forgot_password.body", refCode);
            MailMessage mailMessage = new MailMessage(email, subject, body);
            messageProducer.sendMail(mailMessage);

            verifyService.add("ref-email:" + email, refCode, 60 * 5);

            log.info("Forgot-password verification code sent for user: {}", user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public String forgotPassword(ForgotPassDTO forgotPassDTO) {
        if (!verifyService.containsKey("ref-email:" + forgotPassDTO.getEmail())) {
            log.warn("Forgot-password failed — verification code expired");
            throw new BadRequestException("auth.verification.code_expired");
        }
        if (!verifyService.getValue("ref-email:" + forgotPassDTO.getEmail()).equals(forgotPassDTO.getCode())) {
            log.warn("Forgot-password failed — verification code mismatch");
            throw new BadRequestException("auth.verification.code_mismatch");
        }
        String random = UUID.randomUUID().toString();
        verifyService.add("random:" + random, forgotPassDTO.getEmail(), 5 * 60);
        verifyService.add(random, forgotPassDTO.getEmail(), 5 * 60);

        log.info("Forgot-password verification succeeded — reset token generated");
        return random;
    }

    @Override
    public void checkRandom(String random) {
        if (!StringUtils.hasText(random)) {
            throw new BadRequestException("auth.verification.code_not_found");
        }
        if (!verifyService.containsKey("random:" + random)) {
            log.warn("Reset password random check failed — token not found or expired");
            throw new BadRequestException("auth.verification.email_required");
        }
    }

    @Override
    public void resetPassword(ResetDTO resetDTO) {
        if (!verifyService.containsKey("random:" + resetDTO.getRandom())) {
            log.warn("Password reset failed — reset token expired");
            throw new BadRequestException("auth.verification.code_expired");
        }
        String email = verifyService.getValue(resetDTO.getRandom()).toString();
        User user = userRepository.findByEmail_Value(email)
                .orElseThrow(() -> new BadRequestException("auth.verification.failed"));

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            user.setPassword(new Password(encoder.encode(resetDTO.getNewPass())));
            userRepository.save(user);
            jwtFamilyService.revokeAllUserFamilies(user.getEmail());
            if (userDetailsCacheService != null) {
                userDetailsCacheService.evictByEmail(user.getEmail());
            }
            verifyService.delete("ref-email:" + email);
            verifyService.delete("random:" + resetDTO.getRandom());
            verifyService.delete(resetDTO.getRandom());

            log.info("Password reset completed successfully for user: {}", user.getId());
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public String checkLogin(User user) {
        if (user == null) {
            throw new UnauthorizedException("message.unauthorized");
        }
        return user.getEmail();
    }

    private String registerByRole(RegistrationForm registrationForm, String role) {
        log.info("Registration started for role: {}", role);

        User user = mapper.toUser(registrationForm, encoder, role);
        userRepository.saveAndFlush(user);

        try {
            MDC.put(MDC_USER_ID, String.valueOf(user.getId()));

            sendLinkActivate(user.getUsername());

            log.info("Registration completed for user: {} with role: {}", user.getId(), role);
        } finally {
            MDC.remove(MDC_USER_ID);
        }
        return user.getUsername();
    }

    private void validateAccountRole(User user, String expectedRole) {
        String normalizedExpectedRole = RoleConstants.normalizeRole(expectedRole);
        boolean matched = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(RoleConstants::normalizeRole)
                .anyMatch(normalizedExpectedRole::equals);
        if (!matched) {
            log.warn("Role mismatch — user: {} expected: {}, actual roles: {}",
                    user.getId(), expectedRole, user.getRole());
            throw new ForbiddenException(getAccountRoleMismatchMessage(expectedRole));
        }
    }

    private String getAccountRoleMismatchMessage(String expectedRole) {
        return RoleConstants.ROLE_HIRER.equals(RoleConstants.normalizeRole(expectedRole))
                ? "auth.login.hirer.account_type_invalid"
                : "auth.login.user.account_type_invalid";
    }

    private void validateAccountStatus(User user) {
        if (!user.isAccountNonLocked()) {
            log.warn("Authentication rejected — account locked for user: {}", user.getId());
            throw new ForbiddenException("auth.account.locked");
        }
        if (!user.isEnabled()) {
            log.warn("Authentication rejected — account disabled for user: {}", user.getId());
            throw new ForbiddenException("auth.account.disabled");
        }
        if (!user.isActive()) {
            log.warn("Authentication rejected — account not activated for user: {}", user.getId());
            throw new ForbiddenException("auth.account.not_activated");
        }
    }

    private String createLink(User user) {
        String token = jwtService.generateActivationToken(user.getUsername());
        return url + "/activate?token=" + token;
    }
}
