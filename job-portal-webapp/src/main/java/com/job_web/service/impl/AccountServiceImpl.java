package com.job_web.service.impl;

import com.job_web.data.UserRepository;
import com.job_web.dto.*;
import com.job_web.message.MailProducer;
import com.job_web.models.RefreshToken;
import com.job_web.models.User;
import com.job_web.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
	private final PasswordEncoder encoder;
	private final UserRepository userRepository;
	private final IRefService refService;
	private final IMailService mailService;
	private final IVerifyService verifyService;
	private final MailProducer mailProducer;
	private final RefreshTokenService refreshTokenService;
	private final SpamService spamService;
	private final JwtService jwtService;
	@Value("${application.service.impl.subject-verify}")
	private String subject;
	@Value("${application.service.impl.email-verify}")
	private String textTemplate;
	@Override
	public boolean checkPassword( String passwordInput, String passwordInstored) {
		return encoder.matches(passwordInput, passwordInstored);
	}

	@Override
	public ApiResponse<UserInfo> getDetailUser(Principal principal) {
		if(principal == null) {
			return new ApiResponse<>("Chưa đăng nhập",null,400);
		}
		User userLogin =  userRepository.findByEmail(principal.getName()).orElseThrow(RuntimeException::new);
		UserInfo userInfo = new UserInfo();
		userInfo.toUserInfo(userLogin);
		return new ApiResponse<>("success",userInfo,200);
	}

	@Override
	public ApiResponse<String> changePassword(String newPassword, String oldPassword) {
		User userLogin = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		if(!checkPassword(oldPassword,userLogin.getPassword())){
			return new ApiResponse<>("password hiện tại không khớp",null,400);
		}
		userLogin.setPassword(encoder.encode(newPassword));
		userRepository.save(userLogin);
		return new ApiResponse<>("success",null,200);
	}

	@Override
	public ApiResponse<String> resetPassword(ResetDTO resetDTO) {
		if(!verifyService.containsKey("random:"+resetDTO.getRandom())){
			return new ApiResponse<>("Mã xác thực đã hết hạn",null,400);
		}
		String email = verifyService.getValue(resetDTO.getRandom()).toString();
		Optional<User> user = userRepository.findByEmail(email);
		if(user.isPresent()){
			user.get().setPassword(encoder.encode(resetDTO.getNewPass()));
			userRepository.save(user.get());
			verifyService.delete("ref-email:"+email);
			verifyService.delete("random:"+resetDTO.getRandom());
			verifyService.delete(resetDTO.getRandom());
			return new ApiResponse<>("success",null,200);
		}
		return new ApiResponse<>("Xác thực thất bại",null,400);
	}

	@Override
	public ApiResponse<String> sendCodeForgotPassword(HttpServletRequest request ,String email) {
		String ip = getClientIP(request);
		// kiểm tra spam
		if(spamService.checkIpSpamEmail(ip)){
			return new ApiResponse<>(spamService.getMessageEmailSpam(ip),null,400);
		}
		spamService.addIpSpamEmail(ip);
		// kiểm tra email tồn tại hay không
		if(email == null) {
			return new ApiResponse<>("email rỗng",null,400);
		}
		// kiểm tra có tài khoản nào sử dụng email này không
		if(userRepository.findByEmail(email).isEmpty()) {
			return new ApiResponse<>("email không tồn tại trong hệ thống",null,400);
		}
		String refCode = refService.getRef(6).toUpperCase();
		MailMessage mailMessage = new MailMessage(email, "Mã xác thực quên mật khẩu", "Mã xác thực của bạn là: " + refCode);
		mailProducer.sendMail(mailMessage);

		// put vào server redis lưu trữ ref code trong 5 phút
		verifyService.add("ref-email:" + email, refCode, 60*5);
		return new ApiResponse<>("Chúng tôi đã gửi mã xác thực vào email của bạn",null,200);
	}

	@Override
	public ApiResponse<String> register(RegistationForm registationForm) {
		User user = registationForm.toUser(encoder);
		userRepository.saveAndFlush(user);
		sendLinkActivate(user.getUsername());
		return new ApiResponse<String>("Tạo user thành công. Chuẩn bị sang bước xác nhận tài khoản",
				user.getUsername(), 200);
	}

	@Override
	public ApiResponse<String> sendLinkActivate(String email) {
		Optional<User> user = userRepository.findByEmail(email);
		if(user.isEmpty()){
			return new ApiResponse<>("user không tồn tại trong hệ thống",null,400);
		}
		String link = createLink(email);

		String text = String.format(textTemplate, link);
		MailMessage mailMessage = new MailMessage(email, subject, text);
		try {
			mailProducer.sendMail(mailMessage);
		} catch (Exception e) {
			log.trace(e.getMessage(),e);
			return new ApiResponse<>("Gửi email thất bại", null, 500);
		}
		return null;
	}

	@Override
	public ApiResponse<String> activeAccount(String token) {
		final String activate = jwtService.extractUsername(token);
		String[] s = StringUtils.delimitedListToStringArray(activate, "|");
		Optional<User> user = userRepository.findByEmail(s[0]);
		if(user.isEmpty()){
			return new ApiResponse<>("user không tồn tại trong hệ thống",null,400);
		}
		if (!jwtService.isTokenValid(token, user.get())) {
			new ApiResponse<String>("token hết hạn", null, 200);
		}
		return new ApiResponse<String>("Kích hoạt tài khoản thành công", null, 200);
	}

	@Override
	public ApiResponse<String> login(LoginDTO loginDTO, HttpServletRequest request, HttpServletResponse response) {
		String ip = getClientIP(request);
		if(spamService.checkIpSpamLogin(ip)){
			if(spamService.checkIpSpamLogin(ip)){
				return new ApiResponse<>(spamService.getMessageLoginSpam(ip),null,400);
			}
		}
		Optional<User> user = userRepository.findByEmail(loginDTO.getUsername());
		if(user.isEmpty()){
			return new ApiResponse<>("Email không tồn tại trông hệ thống", null, 400);
		}
		if(!encoder.matches(loginDTO.getPassword(), user.get().getPassword())){
			spamService.addIpSpamLogin(ip);
			return new ApiResponse<>("sai mật khẩu", null, 400);
		}
		spamService.deleteIpSpamLogin(ip);
		String accessToken = jwtService.generateToken(loginDTO.getUsername());
		RefreshToken refreshToken = refreshTokenService.createRefreshToken(loginDTO.getUsername());
		ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
				.httpOnly(true)
				.secure(false)
				.sameSite("Lax")
				.maxAge(Duration.between(Instant.now(), refreshToken.getExpiryDate()).getSeconds())
				.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
		return new ApiResponse<>("đăng nhập thành công", accessToken, 200);
	}

	@Override
	public ApiResponse<String> refreshToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return new ApiResponse<String>("không tìm thấy cookie, đăng nhập lại", null, 400);
		}
		for (Cookie cookie : cookies) {
			if ("refreshToken".equals(cookie.getName())) {
				String token = cookie.getValue();
				Optional<String> accessToken = refreshTokenService.findByToken(token)
						.map(refreshTokenService::verifyExpiration).map(RefreshToken::getUserInfo)
						.map(u -> jwtService.generateToken(u.getUsername()));
				return accessToken.map(s -> new ApiResponse<>("success", s, 200))
						.orElseGet(() -> new ApiResponse<>("phiên bản đăng nhập hết hạn, hãy đăng nhập lại", null, 400));
			}
		}
		return new ApiResponse<String>("không tìm thấy refresh token, đăng nhập lại", null, 400);
	}

	@Override
	public ApiResponse<String> logout(HttpServletRequest request, HttpServletResponse response) {
		Cookie[] cookies = request.getCookies();
		for (Cookie cookie : cookies) {
			if ("refreshToken".equals(cookie.getName())) {
				String token = cookie.getValue();
				refreshTokenService.deleteRefreshToken(token);
				return new ApiResponse<>("success", null, 200);
			}
		}
		response.setHeader("Authorization", "");
		Cookie cookie = new Cookie("token", null);
		cookie.setPath("/");         // Phù hợp với cookie gốc
		cookie.setHttpOnly(true);
		cookie.setSecure(request.isSecure());     // Nếu cookie gốc là secure
		cookie.setMaxAge(0);        // Yêu cầu trình duyệt xóa ngay
		response.addCookie(cookie);
		return new ApiResponse<>("Bạn chưa đăng nhập", null, 400);
	}

	@Override
	public ApiResponse<String> forgotPassword(ForgotPassDTO forgotPassDTO) {
		if(!verifyService.containsKey("ref-email:" + forgotPassDTO.getEmail())){
			return new ApiResponse<>("Mã xác thực hết hạn", null, 400);
		}
		if(!verifyService.getValue("ref-email:" + forgotPassDTO.getEmail()).equals(forgotPassDTO.getCode())){
			return new ApiResponse<>("Mã xác thực không khớp", null, 400);
		}
		String random = UUID.randomUUID().toString();
		verifyService.add("random:"+random, forgotPassDTO.getEmail(),5*60);
		verifyService.add(random, forgotPassDTO.getEmail(),5*60);
		return new ApiResponse<>("success", random, 200);
	}

	@Override
	public ApiResponse<String> checkRandom(String random) {
		if(!StringUtils.hasText(random)){
			return new ApiResponse<>("Mã xác thực không tồn tại", null, 400);
		}
		if(!verifyService.containsKey("random:"+random)){
			return new ApiResponse<>("Bạn phải xác thực email trước khi dùng cài lại mật khẩu", null, 400);
		}
		return new ApiResponse<>("success", null, 200);
	}

	private String createLink(String username) {
		String token = jwtService.generateToken(username+"|activate");
		return "http://localhost:4200/activate?token=" + token;
	}
	private String getClientIP(HttpServletRequest request) {
		String xfHeader = request.getHeader("X-Forwarded-For");
		if (xfHeader == null || xfHeader.isEmpty() || !xfHeader.contains(request.getRemoteAddr())) {
			return request.getRemoteAddr();
		}
		return xfHeader.split(",")[0];
	}
}
