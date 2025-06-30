package com.job_web.dto;

import javax.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginDTO {
	@NotBlank(message = "username không được rỗng")
	private String username;
	@NotBlank(message = "password không được rỗng")
	private String password;
}
