package com.job_web.controller;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.job_web.dto.*;
import com.job_web.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/account", produces = "application/json")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials="true")
@Slf4j
@RequiredArgsConstructor
public class AccountController {
	private final AccountService accountService;

	@GetMapping("/pub/code/{email}")
	public ResponseEntity<ApiResponse<String>> getCodeForgotPass(@PathVariable String email, HttpServletRequest request) {
		ApiResponse<String> res = accountService.sendCodeForgotPassword(request,email);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

//	@PostMapping("/verify")
//	public ResponseEntity<Map<String, String>> verifyCode(@Valid @RequestBody ForgotPassDTO fpDTO) {
//		String email = (String) resource.get("userMail");
//		Map<String, String> result = new HashMap<>();
//		// kiểm tra xem người dùng có nhập đúng email yêu cầu code hay không
//		if (email == null || email.trim().isEmpty()) {
//			result.put("mess", "Email rỗng");
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
//		}
//		// kiểm tra xem code nhập đã hết hạn chưa
//		if (!verifyService.containsKey("ref-email:" + email)) {
//			result.put("mess", "Mã đã hết hạn");
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
//		}
//		// kiểm tra ref code có nhập đúng với email yêu cầu không.
//		if (!resource.get("value").equals(verifyService.getValue("ref-email:" + email))) {
//			result.put("mess", "Mã xác thực không khớp.");
//			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
//		}
//		result.put("mess", "Xác thực thành công");
//		// tạo ra token ngẫu nhiên
//		String token = refService.getRef();
//		// tạo ra link chứa token người dùng có thể truy cập để đổi mật khẩu.
//
//		// thêm link chứa token vào trong redis với timeout là 10p.
//		verifyService.add("recovery:" + token, token, 600);
//		// thêm username vào redis
//		verifyService.add("user-verify:" + token, email, 600);
//		return ResponseEntity.ok(result);
//	}

	@PutMapping("/pri/updateUserInfo")
	public ResponseEntity<ApiResponse<String>> putUpdateUserInfo(@Valid @RequestBody UserInfo userInfo,
			BindingResult bindingResult, Principal principal) {
		if (bindingResult.hasErrors()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(bindingResult.getAllErrors().getFirst().getDefaultMessage(), null, 400));
		}
		ApiResponse<String> res = accountService.updateInfo(userInfo,principal);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@PostMapping("/pub/login")
	public ResponseEntity<ApiResponse<String>> login(@RequestBody LoginDTO login, HttpServletRequest request , HttpServletResponse response, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<>(bindingResult.getAllErrors().getFirst().getDefaultMessage(), null, 400));
		}
		ApiResponse<String> res = accountService.login(login, request, response);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@PostMapping("/pub/register")
	public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegistationForm registationForm,
			BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<String>(bindingResult.getAllErrors().getFirst().getDefaultMessage(), null, 400));
		}
		ApiResponse<String> res = accountService.register(registationForm);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@Async
	@GetMapping("/pri/sendLink/{email}")
	public CompletableFuture<ResponseEntity<ApiResponse<String>>> getLinkVerify(@PathVariable String email) {
		ApiResponse<String> res = accountService.sendLinkActivate(email);
		return CompletableFuture.completedFuture(ResponseEntity.status(res.getStatus()).body(res));
	}

	@GetMapping("/pub/activate/{token}")
	public ResponseEntity<ApiResponse<String>> activateAccount(@PathVariable String token) {
		ApiResponse<String> res = accountService.activeAccount(token);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@GetMapping("/pri/checkLogin")
	public ResponseEntity<ApiResponse<Object>> checkLogin(Principal principal) {
		if(principal != null) {
			return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>("success",null, HttpStatus.OK.value()));
		}
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>("error",null, HttpStatus.BAD_REQUEST.value()));
	}

	@GetMapping("/pub/refreshToken")
	public ResponseEntity<ApiResponse<String>> getRefreshToken(HttpServletRequest request) {
		ApiResponse<String> res = accountService.refreshToken(request);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@GetMapping("/pri/detail")
	public ResponseEntity<ApiResponse<UserInfo>> getDetails(Principal principal) {
		ApiResponse<UserInfo> res = accountService.getDetailUser(principal);
		return ResponseEntity.status(res.getStatus()).body(res);
	}
	@GetMapping("/pub/logout")
	public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
		ApiResponse<String> res = accountService.logout(request,response);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@PutMapping("/pri/changePass")
	public ResponseEntity<ApiResponse<String>> changePass(@RequestBody ChangePassDTO changePassDTO) {
		ApiResponse<String> res = accountService.changePassword(changePassDTO.getNewPass(), changePassDTO.getOldPass());
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@PostMapping("/pub/forgotPass")
	public ResponseEntity<ApiResponse<String>> forgotPass(@RequestBody @Valid ForgotPassDTO forgotPassDTO, BindingResult bindingResult) {
		if(bindingResult.hasErrors()) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(bindingResult.getAllErrors().getFirst().getDefaultMessage(), null, 400));
		}
		ApiResponse<String> res = accountService.forgotPassword(forgotPassDTO);
		return ResponseEntity.status(res.getStatus()).body(res);
	}
	@GetMapping("/pub/url/google")
	public ResponseEntity<ApiResponse<String>> googleUrl(HttpServletRequest req) {
		String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
		String url = base + "/oauth2/authorization/google";
		return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<String>("success",url,HttpStatus.OK.value()));
	}

	@GetMapping("/pub/checkRandom/{random}")
	public ResponseEntity<ApiResponse<String>> checkRandom(@PathVariable String random) {
		ApiResponse<String> res = accountService.checkRandom(random);
		return ResponseEntity.status(res.getStatus()).body(res);
	}

	@PatchMapping("/pub/reset")
	public ResponseEntity<ApiResponse<String>> resetPass(@RequestBody @Valid ResetDTO resetDTO, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return ResponseEntity.badRequest()
					.body(new ApiResponse<String>(bindingResult.getAllErrors().getFirst().getDefaultMessage(), null, 400));
		}
		ApiResponse<String> res = accountService.resetPassword(resetDTO);
		return ResponseEntity.status(res.getStatus()).body(res);
	}
}
