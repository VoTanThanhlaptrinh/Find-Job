package com.nlu.admin.application;

import com.nlu.admin.api.dto.user.UserCrudDTO;
import com.nlu.admin.api.dto.user.UserResponseDTO;
import org.springframework.data.domain.Page;

public interface UserCrudService {
    void createUser(UserCrudDTO userDTO);

    UserResponseDTO getUserByEmail(String email);

    Page<UserResponseDTO> getUsers(int page, int size);

    void updateUser(Long id, UserCrudDTO userDTO);

    void deleteUser(Long id);
}
