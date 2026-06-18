package com.nlu.recruitment.application;

import com.nlu.identity.domain.model.User;
import com.nlu.recruitment.api.dto.AddressRequestDto;
import com.nlu.recruitment.api.dto.AddressResponseDto;

import java.util.List;

public interface AddressService {
    AddressResponseDto createAddress(AddressRequestDto dto, User user);
    AddressResponseDto updateAddress(Long id, AddressRequestDto dto, User user);
    void deleteAddress(Long id, User user);
    List<AddressResponseDto> getAddressesByUser(User user);
}
