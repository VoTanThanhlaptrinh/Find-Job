package com.job_web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePassDTO {
    @NotNull(message = "password hiện tại không được rỗng")
    private String oldPass;
    @NotNull(message = "password mới không được rỗng")
    @Size(min = 8, message = "mật khẩu tối thiểu 8 ký tự")
    private String newPass;
    @NotNull(message = "password nhập lại không được rỗng")
    private String confirmPass;
    @AssertTrue(message = "password mới và password nhập lại phải khớp nhau")
    public boolean isPasswordsMatch() {
        return newPass != null && newPass.equals(confirmPass);
    }
}
