package com.job_web.dto;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.job_web.custom.EmailExist;
import com.job_web.models.User;

import lombok.Data;

@Data

public class RegistationForm {
	@NotBlank(message = "Bạn phải chọn vai trò của tài khoản đăng nhập")
	private String role;
	@NotBlank(message = "Tên đầy đủ không được rỗng")
	@Size(max = 255, message = "Tên đầy đủ chỉ được tối đa 255 ký tự")
	private String fullName;
	@NotBlank(message = "Tên tài khoản không được rỗng")
	@Email(message = "Không phải Email")
	@EmailExist
	private String username;
	@NotBlank(message = "Mật khẩu không được rỗng")
	@Size(min = 8, message = "Mật khẩu không được dưới 8 ký tự")
	private String password;
	@NotBlank(message = "xác nhận mật khẩu không được rỗng")
	private String confirmPassword;

	@AssertTrue(message = "Mật khẩu và xác nhận mật khẩu không khớp")
	public boolean isPasswordMatch() {
		return password != null && password.equals(confirmPassword);
	}

	public User toUser(PasswordEncoder passwordEncoder) {
		User user = new User();
		user.setPassword(passwordEncoder.encode(password));
		user.setEmail(username);
		user.setFullName(fullName);
		user.setAddress("");
		user.setMobile("");
		user.setRole(role);
		user.setActive(false);
		user.setEnabled(true);
		return user;
	}
}
