package com.job_web.service.impl;

import com.job_web.data.UserRepository;
import com.job_web.dto.ApiResponse;
import com.job_web.dto.UserInfo;
import com.job_web.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.job_web.service.AccountService;
@Service
@RequiredArgsConstructor
public class AccountServiceImp implements AccountService {
	private final PasswordEncoder encoder;
	private final UserRepository userRepository;
	@Override
	public boolean checkPassword( String passwordInput, String passwordInstored) {
		// TODO Auto-generated method stub
		return encoder.matches(passwordInput, passwordInstored);
	}

	@Override
	public ApiResponse<UserInfo> getDetailUser() {
		User userLogin = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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

}
