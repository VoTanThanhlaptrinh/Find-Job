package com.job_web.dto;

import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class ResetDTO {
    @Size(min = 8, message = "Mật khẩu không được dưới 8 ký tự")
    @NotBlank(message = "Mật khẩu mới không được rỗng")
    private String newPass;
    @NotBlank(message = "Xác nhận mật khẩu mới không được rỗng")
    private String confirmPass;
    @NotBlank(message = "Mã xác nhận không được rỗng")
    private String random;

    @AssertTrue(message = "Mật khẩu mới và mật khẩu xác nhận không giống nhau")
    public boolean isValid() {
        return newPass.equals(confirmPass);
    }
}
