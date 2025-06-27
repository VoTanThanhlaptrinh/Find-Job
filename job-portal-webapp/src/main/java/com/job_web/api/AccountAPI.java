package com.job_web.api;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.job_web.dto.*;
import com.job_web.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.job_web.data.UserRepository;
import com.job_web.message.MailProducer;
import com.job_web.models.RefreshToken;
import com.job_web.models.User;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping(path = "/api/account", produces = "application/json")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials="true")
@Slf4j
@AllArgsConstructor
public class AccountAPI {

	private final IRefService refService;

	private final IMailService mailService;

	private final IVerifyService verifyService;

	private UserRepository userRepository;
	private final AccountService accountService;
	private JwtService jwtService;
	private PasswordEncoder encoder;
	private MailProducer mailProducer;
	private final String subject = "Kích hoạt tài khoản JobList của bạn";
	private final AuthenticationManager authenticationManager;
	private final RefreshTokenService refreshTokenService;
	private final String textTemplate = """
			Xin chào bạn,

			Cảm ơn bạn đã đăng ký tài khoản tại JobList.

			Để hoàn tất quá trình đăng ký, vui lòng nhấn vào liên kết dưới đây để kích hoạt tài khoản:
			%s

			Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email này.

			Trân trọng,
			JobList Team
			""";

	@GetMapping("/code/{email}")
	public ResponseEntity<Map<String, Object>> getEmail(@PathVariable String email) {
		// response chứa thông tin trả về
		Map<String, Object> res = new HashMap<>();
		if (email == null) { // kiểm tra xem email có rỗng hay không
			res.put("mess", "Email rỗng");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
		}
		if (userRepository.findByEmail(email).isEmpty()) {
			// kiểm tra email có nằm trong hệ thống hay không.
			res.put("mess", "Không tìm thấy email trong hệ thống");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
		}
		// tạo ra ref code để xác thực
		String refCode = refService.getRef(6).toUpperCase();
		try {
			// gửi ref code tới email user.
			mailService.sendMessage(email, "Mã xác thực quên mật khẩu", "Mã xác thực của bạn là: " + refCode);
			// gửi thông báo thành công.
			res.put("mess", "Gửi thành công");
			// put vào server redis lưu trữ ref code trong 60s
			verifyService.add("ref-email:" + email, refCode, 60);
		} catch (Exception e) {
			// bắt ngoại lệ khi gửi qua mail.
			log.error(e.getMessage());
			e.printStackTrace();
			res.put("mess", "Lỗi gửi mã");
			return ResponseEntity.badRequest().body(res);
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(res);
	}

	@PostMapping("/verify")
	public ResponseEntity<Map<String, String>> verifyCode(@RequestBody Map<String, Object> resource) {
		String email = (String) resource.get("userMail");
		Map<String, String> result = new HashMap<>();
		// kiểm tra xem người dùng có nhập đúng email yêu cầu code hay không
		if (email == null || email.trim().isEmpty()) {
			result.put("mess", "Email rỗng");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
		}
		// kiểm tra xem code nhập đã hết hạn chưa
		if (!verifyService.containsKey("ref-email:" + email)) {
			result.put("mess", "Mã đã hết hạn");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
		}
		// kiểm tra ref code có nhập đúng với email yêu cầu không.
		if (!resource.get("value").equals(verifyService.getValue("ref-email:" + email))) {
			result.put("mess", "Mã xác thực không khớp.");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
		}
		result.put("mess", "Xác thực thành công");
		// tạo ra token ngẫu nhiên
		String token = refService.getRef();
		// tạo ra link chứa token người dùng có thể truy cập để đổi mật khẩu.

		// thêm link chứa token vào trong redis với timeout là 10p.
		verifyService.add("recovery:" + token, token, 600);
		// thêm username vào redis
		verifyService.add("user-verify:" + token, email, 600);
		return ResponseEntity.ok(result);
	}

	private String createLink(String username) {
		String token = jwtService.generateToken(username);
		return "http://localhost:4200/activate?token=" + token;
	}

	@PostMapping("/updateUserInfo")
	public ResponseEntity<Map<String, String>> patchUpdateUserInfo(@Valid @RequestBody UserInfo userInfo,
			BindingResult bindingResult, Principal principal) {
		if (bindingResult.hasErrors()) {
			Map<String, String> req = new HashMap<>();
			req.put("message", bindingResult.getAllErrors().get(0).getDefaultMessage());
			return ResponseEntity.badRequest().body(req);
		}
		return ResponseEntity.ok().build();
	}

	@PostMapping("/login")
	public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginDTO login, HttpServletResponse response) {
		// TODO: process POST request
		Authentication authentication = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(login.getUsername(), login.getPassword()));
		if (authentication.isAuthenticated()) {
			String accessToken = jwtService.generateToken(login.getUsername());
			RefreshToken refreshToken = refreshTokenService.createRefreshToken(login.getUsername());
			ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken.getToken())
					.httpOnly(true)
					.secure(false)
					.sameSite("Lax")
					.maxAge(Duration.between(Instant.now(), refreshToken.getExpiryDate()).getSeconds())
							.build();
			response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

			return ResponseEntity.ok().body(new ApiResponse<String>("đăng nhập thành công", accessToken, 200));
		} else {
			return ResponseEntity.badRequest().body(new ApiResponse<String>("sai mật khẩu", null, 400));
		}

	}

	@PostMapping("/register")
	public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegistationForm registationForm,
			BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<String>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
		}
		User user = registationForm.toUser(encoder);
		userRepository.saveAndFlush(user);
		getLinkVerify(user.getUsername());
		return ResponseEntity.ok(new ApiResponse<String>("Tạo user thành công. Chuẩn bị sang bước xác nhận tài khoản",
				user.getUsername(), 200));
	}

	@Async
	@GetMapping("/sendLink/{username}")
	public ResponseEntity<ApiResponse<String>> getLinkVerify(@PathVariable String username) {
		User user = userRepository.findByEmail(username).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Không tìm thấy username trong hệ thống"));

		String link = createLink(user.getUsername());

		String text = String.format(textTemplate, link);
		MailMessage mailMessage = new MailMessage(username, subject, text);
		try {
			mailProducer.sendMail(mailMessage);
		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ApiResponse<>("Gửi email thất bại", null, 500));
		}
		return ResponseEntity.ok().build();
	}

	@GetMapping("/activate/{token}")
	public ResponseEntity<ApiResponse<String>> activateAccount(@PathVariable String token) {
		final String username = jwtService.extractUsername(token);
		User user = userRepository.findByEmail(username)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Không tìm thấy username này trong hệ thống"));
		if (!jwtService.isTokenValid(token, user)) {
			return ResponseEntity.badRequest().body(new ApiResponse<String>("token hết hạn", null, 200));
		}
		return ResponseEntity.ok(new ApiResponse<String>("Kích hoạt tài khoản thành công", null, 200));
	}

	@GetMapping("/checkLogin")
	public ResponseEntity<ApiResponse<Object>> checkLogin() {
		if(SecurityContextHolder.getContext().getAuthentication().isAuthenticated()){
			return ResponseEntity.ok().build();
		}
		return ResponseEntity.badRequest().build();
	}

	@GetMapping("/refreshToken")
	public ResponseEntity<ApiResponse<String>> getRefreshToken(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return ResponseEntity.badRequest().body(new ApiResponse<String>("cookie not found", null, 400));
		}
		for (Cookie cookie : cookies) {
			if ("refreshToken".equals(cookie.getName())) {
				String token = cookie.getValue();
				Optional<String> accessToken = refreshTokenService.findByToken(token)
						.map(refreshTokenService::verifyExpiration).map(RefreshToken::getUserInfo)
						.map(u -> jwtService.generateToken(u.getUsername()));
                return accessToken.map(s -> ResponseEntity.ok(new ApiResponse<>("success", s, 200)))
						.orElseGet(() -> ResponseEntity.badRequest()
								.body(new ApiResponse<>("refresh token invalid", null, 400)));
            }
		}
		return ResponseEntity.badRequest().body(new ApiResponse<String>("refresh not found", null, 400));
	}

	@GetMapping("/detail")
	public ResponseEntity<ApiResponse<UserInfo>> getDetails() {
		ApiResponse<UserInfo> res = accountService.getDetailUser();
		return ResponseEntity.ok().body(res);
	}

	@PostMapping("/changePass")
	public ResponseEntity<ApiResponse<String>> changePass(@RequestBody ChangePassDTO changePassDTO) {
		ApiResponse<String> res = accountService.changePassword(changePassDTO.getOldPass(), changePassDTO.getNewPass());
		return ResponseEntity.status(res.getStatus()).body(res);
	}

}
