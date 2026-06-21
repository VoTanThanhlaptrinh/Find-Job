package com.nlu.recruitment.application.impl;

import com.nlu.identity.domain.model.User;
import com.nlu.recruitment.api.dto.AddressRequestDto;
import com.nlu.recruitment.api.dto.AddressResponseDto;
import com.nlu.recruitment.application.AddressService;
import com.nlu.recruitment.domain.model.Address;
import com.nlu.recruitment.domain.model.Recruitment;
import com.nlu.recruitment.domain.repository.AddressRepository;
import com.nlu.recruitment.domain.repository.RecruitmentRepository;
import com.nlu.shared.domain.exception.ForbiddenException;
import com.nlu.shared.domain.exception.ResourceNotFoundException;
import com.nlu.shared.utils.MessageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {
    private final AddressRepository addressRepository;
    private final RecruitmentRepository recruitmentRepository;

    @Override
    @Transactional
    public AddressResponseDto createAddress(AddressRequestDto dto, User user) {
        Recruitment recruitment = getRecruitmentByUser(user);

        handleDefaultAddress(recruitment.getAddresses(), dto.getIsDefault(), null);

        Address address = new Address();
        address.setLocationName(dto.getLocationName());
        address.setCity(dto.getCity());
        address.setStreet(dto.getStreet());
        address.setIsDefault(dto.getIsDefault());

        address = addressRepository.save(address);

        recruitment.getAddresses().add(address);
        recruitmentRepository.save(recruitment);

        return toDto(address);
    }

    @Override
    @Transactional
    public AddressResponseDto updateAddress(Long id, AddressRequestDto dto, User user) {
        Recruitment recruitment = getRecruitmentByUser(user);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("message.not_found"));

        if (!recruitment.isExistAddress(address)) {
            throw new ForbiddenException("message.forbidden");
        }

        handleDefaultAddress(recruitment.getAddresses(), dto.getIsDefault(), id);

        address.setLocationName(dto.getLocationName());
        address.setCity(dto.getCity());
        address.setStreet(dto.getStreet());
        address.setIsDefault(dto.getIsDefault());

        address = addressRepository.save(address);

        return toDto(address);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id, User user) {
        Recruitment recruitment = getRecruitmentByUser(user);

        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("message.not_found"));

        if (!recruitment.isExistAddress(address)) {
            throw new ForbiddenException("message.forbidden");
        }

        address.markDeleted();
        addressRepository.save(address);
    }

    @Override
    public List<AddressResponseDto> getAddressesByUser(User user) {
        Recruitment recruitment = getRecruitmentByUser(user);
        return recruitment.getAddresses().stream()
                .filter(a -> a.getRecordStatus() != com.nlu.shared.domain.model.EntityStatus.DELETED)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private Recruitment getRecruitmentByUser(User user) {
        return recruitmentRepository.findRecruitmentByUser(user)
                .orElseThrow(() -> new ForbiddenException("message.forbidden"));
    }

    private void handleDefaultAddress(List<Address> addresses, Boolean isDefault, Long excludeId) {
        if (Boolean.TRUE.equals(isDefault)) {
            for (Address addr : addresses) {
                if (!addr.getId().equals(excludeId) && Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    addressRepository.save(addr);
                }
            }
        }
    }

    private AddressResponseDto toDto(Address address) {
        return new AddressResponseDto(
                address.getId(),
                address.getLocationName(),
                address.getCity(),
                address.getStreet(),
                address.getIsDefault()
        );
    }
}
