package com.job_web.service.user;

import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.user.UserCrudDTO;
import com.job_web.dto.user.UserResponseDTO;
import org.springframework.data.domain.Page;

public interface UserCrudService {
    ApiResponse<String> createUser(UserCrudDTO userDTO);

    ApiResponse<UserResponseDTO> getUserById(long id);

    ApiResponse<Page<UserResponseDTO>> getUsers(int pageIndex, int pageSize);

    ApiResponse<String> updateUser(long id, UserCrudDTO userDTO);

    ApiResponse<String> deleteUser(long id);
}
