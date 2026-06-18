package com.nlu.recruitment.api;

import com.nlu.identity.domain.model.CurrentUser;
import com.nlu.identity.domain.model.User;
import com.nlu.recruitment.api.dto.AddressJobCount;
import com.nlu.recruitment.api.dto.AddressRequestDto;
import com.nlu.recruitment.api.dto.AddressResponseDto;
import com.nlu.recruitment.application.AddressService;
import com.nlu.recruitment.infrastructure.cache.CategoryCacheService;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.shared.utils.MessageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/addresses", produces = "application/json")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final CategoryCacheService categoryCacheService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponseDto>>> getMyAddresses(@CurrentUser User user) {
        List<AddressResponseDto> data = addressService.getAddressesByUser(user);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponseDto>> createAddress(
            @Valid @RequestBody AddressRequestDto dto,
            @CurrentUser User user) {
        AddressResponseDto data = addressService.createAddress(dto, user);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponseDto>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequestDto dto,
            @CurrentUser User user) {
        AddressResponseDto data = addressService.updateAddress(id, dto, user);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long id,
            @CurrentUser User user) {
        addressService.deleteAddress(id, user);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), null, HttpStatus.OK.value()));
    }

    @GetMapping("/address-count")
    public ResponseEntity<ApiResponse<List<AddressJobCount>>> getAddressCount() {
        List<AddressJobCount> data = categoryCacheService.getCategoryData();
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }
}
