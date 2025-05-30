package com.job_web.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UserInfo {
	@NotBlank(message = "Tên đầy đủ không được rỗng")
	@Size(max = 254, message = "Tên đầy đủ quá dài")
	private String fullname;
	@NotBlank(message = "Số điện thoại không được rỗng")
	@Size(max = 10, min = 10, message = "Số điện thoại phải chính xác 10 số")
	private String mobile;
	@NotBlank(message = "Địa chỉ không được rỗng")
	@Size(max = 255, message = "Địa chỉ quá dài")
	private String address;

}
