package com.nlu.recruitment.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponseDto {
    private Long id;
    private String locationName;
    private String city;
    private String street;
    private Boolean isDefault;
    public String getFullAddress() {
        return (street != null ? street : "") + ", " + (city != null ? city : "");
    }
}
