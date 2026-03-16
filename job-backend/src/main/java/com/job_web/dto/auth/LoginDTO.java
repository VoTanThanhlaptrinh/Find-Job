package com.job_web.dto.auth;

import jakarta.validation.constraints.*;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginDTO {
	@NotBlank(message = "Bạn phải chọn vai trò của tài khoản đăng nhập")
	private String role;
	@NotBlank(message = "username không được rỗng")
	private String username;
	@NotBlank(message = "password không được rỗng")
	private String password;
}





