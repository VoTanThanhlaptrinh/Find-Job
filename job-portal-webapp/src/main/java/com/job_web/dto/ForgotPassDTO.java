package com.job_web.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class ForgotPassDTO {
    @NotNull(message = "email rỗng")
    private String email;
    @NotNull(message = "mã xác thực rỗng")
    private String code;
}
