package com.job_web.service.user.impl;

import com.job_web.data.UserRepository;
import com.job_web.dto.user.UserCrudDTO;
import com.job_web.dto.user.UserResponseDTO;
import com.job_web.exception.BadRequestException;
import com.job_web.exception.ResourceNotFoundException;
import com.job_web.models.User;
import com.job_web.service.user.UserCrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCrudServiceImpl implements UserCrudService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void createUser(UserCrudDTO userDTO) {
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new BadRequestException("account.email.exists");
        }
        User user = new User();
        applyDtoToUser(user, userDTO, true);
        userRepository.save(user);
    }

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("auth.user.not_found"));
    }

    @Override
    public Page<UserResponseDTO> getUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size)).map(this::toResponse);
    }

    @Override
    public void updateUser(Long id, UserCrudDTO userDTO) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("auth.user.not_found"));

        if (!user.getEmail().equals(userDTO.getEmail()) && userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            throw new BadRequestException("account.email.exists");
        }
        applyDtoToUser(user, userDTO, false);
        userRepository.save(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("auth.user.not_found"));
        user.markDeleted();
        user.setActive(false);
        user.setEnabled(false);
        userRepository.save(user);
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
