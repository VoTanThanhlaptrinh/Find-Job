package com.nlu.recruitment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddressRequestDto {
    @NotBlank(message = "validation.address.city.required")
    private String city;

    @NotBlank(message = "validation.address.street.required")
    private String street;

    @NotNull(message = "validation.address.isDefault.required")
    private Boolean isDefault;
}
