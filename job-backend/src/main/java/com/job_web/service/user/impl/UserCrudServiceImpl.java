package com.job_web.service.user.impl;

import com.job_web.data.UserRepository;
import com.job_web.dto.user.UserCrudDTO;
import com.job_web.dto.user.UserResponseDTO;
import com.job_web.exception.BadRequestException;
import com.job_web.exception.ResourceNotFoundException;
import com.job_web.models.User;
import com.job_web.service.user.UserCrudService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCrudServiceImpl implements UserCrudService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final String MDC_USER_ID = "userId";

    @Override
    public void createUser(UserCrudDTO userDTO) {
        if (userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
            log.warn("User creation failed — email already exists");
            throw new BadRequestException("account.email.exists");
        }
        User user = new User();
        applyDtoToUser(user, userDTO, true);
        userRepository.save(user);

        log.info("User created — user: {}, role: {}", user.getId(), user.getRole());
    }

    @Override
    public UserResponseDTO getUserByEmail(String email) {
        // Read-only — no logging needed.
        return userRepository.findByEmail(email)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("auth.user.not_found"));
    }

    @Override
    public Page<UserResponseDTO> getUsers(int page, int size) {
        // Read-only — no logging needed.
        return userRepository.findAll(PageRequest.of(page, size)).map(this::toResponse);
    }

    @Override
    public void updateUser(Long id, UserCrudDTO userDTO) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(id));

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("auth.user.not_found"));

            if (!user.getEmail().equals(userDTO.getEmail()) && userRepository.findByEmail(userDTO.getEmail()).isPresent()) {
                log.warn("User update failed — target email already exists for user: {}", id);
                throw new BadRequestException("account.email.exists");
            }

            applyDtoToUser(user, userDTO, false);
            userRepository.save(user);

            log.info("User updated — user: {}, role: {}", id, user.getRole());
        } finally {
            MDC.remove(MDC_USER_ID);
        }
    }

    @Override
    public void deleteUser(Long id) {
        try {
            MDC.put(MDC_USER_ID, String.valueOf(id));

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("auth.user.not_found"));

            user.markDeleted();
            user.setActive(false);
            user.setEnabled(false);
            userRepository.save(user);

            log.info("User soft-deleted — user: {}", id);
        } finally {
            MDC.remove(MDC_USER_ID);
        }
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
