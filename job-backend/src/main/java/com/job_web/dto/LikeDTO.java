package com.job_web.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LikeDTO {
    @NotNull(message = "Blog id không được null")
    private long id;
}
