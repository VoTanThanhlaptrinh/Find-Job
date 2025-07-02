package com.job_web.dto;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import antlr.StringUtils;
import com.job_web.models.Apply;
import com.job_web.models.CV;
import com.job_web.models.Token;
import com.job_web.models.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class UserInfo {
	@NotNull(message = "Tên đầy đủ không được rỗng")
	private String fullName;
	@NotNull(message = "Ngày sinh không được rỗng")
	private LocalDate dateOfBirth;
	@NotNull(message = "Địa chỉ không được rỗng")
	private String address;
	@NotNull(message = "Số điện thoại không được rỗng")
	private String mobile;

	@AssertTrue(message = "không phải đinh dạng số điện thoại")
	public boolean isMobile() {
		Pattern pattern = Pattern.compile("^\\d{10}$");
		Matcher matcher = pattern.matcher(mobile);
		return matcher.matches();
	}

	public void toUserInfo(User userLogin) {
		this.fullName = userLogin.getFullName();
		this.address = userLogin.getAddress();
		this.mobile = userLogin.getMobile();
		this.dateOfBirth = userLogin.getDateOfBirth();
	}
	public void update(User userLogin) {
		userLogin.setFullName(fullName);
		userLogin.setDateOfBirth(dateOfBirth);
		userLogin.setAddress(address);
		userLogin.setMobile(mobile);
	}
}
