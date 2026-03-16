package com.job_web.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserCrudDTO {
    @NotBlank(message = "Tên đầy đủ không được rỗng")
    @Size(max = 255, message = "Tên đầy đủ tối đa 255 ký tự")
    private String fullName;

    @NotBlank(message = "Email không được rỗng")
    @Email(message = "Không phải Email")
    private String email;

    @NotBlank(message = "Mật khẩu không được rỗng")
    @Size(min = 8, message = "Mật khẩu không được dưới 8 ký tự")
    private String password;

    @NotBlank(message = "Vai trò không được rỗng")
    private String role;

    @NotNull(message = "Ngày sinh không được rỗng")
    @Past(message = "Ngày sinh phải trước hôm nay")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Địa chỉ không được rỗng")
    private String address;

    @NotBlank(message = "Số điện thoại không được rỗng")
    @Pattern(regexp = "^\\d{10}$", message = "Không phải định dạng số điện thoại")
    private String mobile;

    @NotNull(message = "Trạng thái kích hoạt không được rỗng")
    private Boolean active;

    @NotNull(message = "Trạng thái khóa tài khoản không được rỗng")
    private Boolean accountLocked;

    @NotNull(message = "Trạng thái enable không được rỗng")
    private Boolean enabled;

    @NotNull(message = "Trạng thái OAuth2 không được rỗng")
    private Boolean oauth2Enabled;
}
