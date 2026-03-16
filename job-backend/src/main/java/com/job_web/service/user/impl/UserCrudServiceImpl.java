package com.job_web.service.user.impl;

import com.job_web.data.UserRepository;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.user.UserCrudDTO;
import com.job_web.dto.user.UserResponseDTO;
import com.job_web.models.User;
import com.job_web.service.user.UserCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserCrudServiceImpl implements UserCrudService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public ApiResponse<String> createUser(UserCrudDTO userDTO) {
        Optional<User> existing = userRepository.findByEmail(userDTO.getEmail());
        if (existing.isPresent()) {
            return new ApiResponse<>("Email đã tồn tại", null, HttpStatus.BAD_REQUEST.value());
        }
        User user = new User();
        applyDtoToUser(user, userDTO, true);
        userRepository.save(user);
        return new ApiResponse<>("Thành công", null, HttpStatus.CREATED.value());
    }

    @Override
    public ApiResponse<UserResponseDTO> getUserById(long id) {
        return userRepository.findById(id)
                .map(user -> new ApiResponse<>("success", toResponse(user), HttpStatus.OK.value()))
                .orElseGet(() -> new ApiResponse<>("Không tìm thấy user", null, HttpStatus.NOT_FOUND.value()));
    }

    @Override
    public ApiResponse<Page<UserResponseDTO>> getUsers(int pageIndex, int pageSize) {
        Page<UserResponseDTO> page = userRepository.findAll(PageRequest.of(pageIndex, pageSize)).map(this::toResponse);
        return new ApiResponse<>("success", page, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> updateUser(long id, UserCrudDTO userDTO) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("Không tìm thấy user", null, HttpStatus.NOT_FOUND.value());
        }
        User user = userOpt.get();
        if (!user.getEmail().equals(userDTO.getEmail())) {
            Optional<User> existEmail = userRepository.findByEmail(userDTO.getEmail());
            if (existEmail.isPresent()) {
                return new ApiResponse<>("Email đã tồn tại", null, HttpStatus.BAD_REQUEST.value());
            }
        }
        applyDtoToUser(user, userDTO, false);
        userRepository.save(user);
        return new ApiResponse<>("Thành công", null, HttpStatus.OK.value());
    }

    @Override
    public ApiResponse<String> deleteUser(long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return new ApiResponse<>("Không tìm thấy user", null, HttpStatus.NOT_FOUND.value());
        }
        userRepository.delete(userOpt.get());
        return new ApiResponse<>("Thành công", null, HttpStatus.OK.value());
    }

    private void applyDtoToUser(User user, UserCrudDTO dto, boolean isCreate) {
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setRole(dto.getRole());
        user.setDateOfBirth(dto.getDateOfBirth());
        user.setAddress(dto.getAddress());
        user.setMobile(dto.getMobile());
        user.setAccountLocked(Boolean.TRUE.equals(dto.getAccountLocked()));
        user.setActive(Boolean.TRUE.equals(dto.getActive()));
        user.setEnabled(Boolean.TRUE.equals(dto.getEnabled()));
        user.setOauth2Enabled(Boolean.TRUE.equals(dto.getOauth2Enabled()));
        if (isCreate || dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
    }

    private UserResponseDTO toResponse(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getDateOfBirth(),
                user.getAddress(),
                user.getMobile(),
                user.isAccountLocked(),
                user.isEnabled(),
                user.isActive(),
                user.isOauth2Enabled(),
                user.getCreateDate(),
                user.getLastModifiedDate()
        );
    }
}
