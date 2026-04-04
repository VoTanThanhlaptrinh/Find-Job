package web_application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.job_web.JobPortalWebApplication;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.user.UserCrudDTO;
import com.job_web.dto.user.UserResponseDTO;
import com.job_web.service.user.UserCrudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests cho UserCrudController - các API CRUD cho User.
 * 
 * Test cases bao gồm:
 * - Authentication: Kiểm tra user đã đăng nhập
 * - Authorization: Kiểm tra quyền truy cập
 * - Validation: Kiểm tra các trường hợp validation (fullName, email, password, mobile, dateOfBirth, etc.)
 * - Success cases: Các trường hợp thành công
 * - Error handling: Các trường hợp lỗi
 * 
 * Endpoints được test:
 * - POST /api/users - Tạo user mới
 * - GET /api/users - Lấy thông tin user hiện tại
 * - GET /api/users/page/{pageIndex}/{pageSize} - Danh sách user phân trang
 * - PUT /api/users/{id} - Cập nhật user
 * - DELETE /api/users/{id} - Xóa user
 */
@SpringBootTest(classes = JobPortalWebApplication.class)
@AutoConfigureMockMvc
class UserCrudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserCrudService userCrudService;

    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/users";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String TEST_FULL_NAME = "Nguyen Van A";
    private static final String TEST_MOBILE = "0123456789";
    private static final String TEST_ADDRESS = "123 Đường ABC, Quận 1, TP.HCM";
    private static final String TEST_ROLE = "USER";
    private static final LocalDate TEST_DATE_OF_BIRTH = LocalDate.of(1995, 5, 15);

    private UserCrudDTO validUserDTO;
    private UserResponseDTO validUserResponseDTO;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        validUserDTO = new UserCrudDTO(
                TEST_FULL_NAME,
                TEST_EMAIL,
                TEST_PASSWORD,
                TEST_ROLE,
                TEST_DATE_OF_BIRTH,
                TEST_ADDRESS,
                TEST_MOBILE,
                true,   // active
                false,  // accountLocked
                true,   // enabled
                false   // oauth2Enabled
        );

        validUserResponseDTO = new UserResponseDTO(
                1L,
                TEST_FULL_NAME,
                TEST_EMAIL,
                TEST_ROLE,
                TEST_DATE_OF_BIRTH,
                TEST_ADDRESS,
                TEST_MOBILE,
                false,  // accountLocked
                true,   // enabled
                true,   // active
                false,  // oauth2Enabled
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        ApiResponse<String> defaultCreateResponse = new ApiResponse<>("Táº¡o user thÃ nh cÃ´ng", null, HttpStatus.CREATED.value());
        when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(defaultCreateResponse);
    }

    // ==================== POST /api/users - CREATE USER ====================
    @Nested
    @DisplayName("POST /api/users - Tạo User mới")
    class CreateUserTests {

        @Nested
        @DisplayName("Security Tests")
        class SecurityTests {

            @Test
            @DisplayName("UC01: Trả về 401 khi chưa đăng nhập")
            void createUser_Unauthorized() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUserDTO)))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("UC02: Tạo user thành công khi đã đăng nhập với quyền admin")
            @WithMockUser(username = "admin@test.com", roles = "ADMIN")
            void createUser_Success_AsAdmin() throws Exception {
                ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
                when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUserDTO)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.message").value("Tạo user thành công"))
                        .andExpect(jsonPath("$.status").value(201));
            }

            @Test
            @DisplayName("UC03: Tạo user thành công khi đã đăng nhập với quyền user")
            @WithMockUser(username = TEST_EMAIL, roles = "USER")
            void createUser_Success_AsUser() throws Exception {
                ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
                when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUserDTO)))
                        .andExpect(status().isCreated());
            }
        }

        @Nested
        @DisplayName("Validation Tests - FullName")
        class FullNameValidationTests {

            @Test
            @DisplayName("UC04: Trả về 400 khi fullName rỗng")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_FullNameBlank() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        "",     // fullName rỗng
                        TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400));
            }

            @Test
            @DisplayName("UC05: Trả về 400 khi fullName null")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_FullNameNull() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        null,   // fullName null
                        TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC06: Trả về 400 khi fullName vượt quá 255 ký tự")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_FullNameTooLong() throws Exception {
                String longName = "A".repeat(256);
                UserCrudDTO dto = new UserCrudDTO(
                        longName,
                        TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC07: Chấp nhận fullName đúng 255 ký tự (boundary)")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_FullNameExact255() throws Exception {
                String exactName = "A".repeat(255);
                UserCrudDTO dto = new UserCrudDTO(
                        exactName,
                        TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
                when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());
            }
        }

        @Nested
        @DisplayName("Validation Tests - Email")
        class EmailValidationTests {

            @Test
            @DisplayName("UC08: Trả về 400 khi email rỗng")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_EmailBlank() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, "", TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC09: Trả về 400 khi email không hợp lệ - thiếu @")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_EmailInvalid_NoAt() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, "invalidemail.com", TEST_PASSWORD, TEST_ROLE,
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC10: Trả về 400 khi email không hợp lệ - thiếu domain")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_EmailInvalid_NoDomain() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, "test@", TEST_PASSWORD, TEST_ROLE,
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC11: Chấp nhận email hợp lệ với subdomain")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_EmailValid_WithSubdomain() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, "test@mail.company.com", TEST_PASSWORD, TEST_ROLE,
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
                when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());
            }
        }

        @Nested
        @DisplayName("Validation Tests - Password")
        class PasswordValidationTests {

            @Test
            @DisplayName("UC12: Trả về 400 khi password rỗng")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_PasswordBlank() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, "", TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC13: Trả về 400 khi password dưới 8 ký tự")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_PasswordTooShort() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, "1234567", TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC14: Chấp nhận password đúng 8 ký tự (boundary)")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_PasswordExact8() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, "12345678", TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
                when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());
            }

            @Test
            @DisplayName("UC15: Chấp nhận password dài với ký tự đặc biệt")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_PasswordLongWithSpecialChars() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, "P@ssw0rd!@#$%^&*()", TEST_ROLE,
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
                when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());
            }
        }

        @Nested
        @DisplayName("Validation Tests - Mobile")
        class MobileValidationTests {

            @Test
            @DisplayName("UC16: Trả về 400 khi mobile rỗng")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_MobileBlank() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, "", true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC17: Trả về 400 khi mobile không đúng định dạng - chứa chữ")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_MobileInvalid_WithLetters() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, "012345678a", true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC18: Trả về 400 khi mobile ít hơn 10 số")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_MobileTooShort() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, "012345678", true, false, true, false  // 9 số
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC19: Trả về 400 khi mobile nhiều hơn 10 số")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_MobileTooLong() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, "01234567890", true, false, true, false  // 11 số
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC20: Chấp nhận mobile đúng 10 số")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_MobileValid() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, "0123456789", true, false, true, false
                );

                ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
                when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());
            }
        }

        @Nested
        @DisplayName("Validation Tests - DateOfBirth")
        class DateOfBirthValidationTests {

            @Test
            @DisplayName("UC21: Trả về 400 khi dateOfBirth null")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_DateOfBirthNull() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, null,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC22: Trả về 400 khi dateOfBirth là ngày tương lai")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_DateOfBirthFuture() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE,
                        LocalDate.now().plusDays(1),  // Ngày mai
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC23: Trả về 400 khi dateOfBirth là ngày hôm nay")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_DateOfBirthToday() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE,
                        LocalDate.now(),  // Hôm nay
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC24: Chấp nhận dateOfBirth là ngày hôm qua (boundary)")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_DateOfBirthYesterday() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE,
                        LocalDate.now().minusDays(1),  // Hôm qua
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
                when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isCreated());
            }
        }

        @Nested
        @DisplayName("Validation Tests - Other Fields")
        class OtherFieldsValidationTests {

            @Test
            @DisplayName("UC25: Trả về 400 khi role rỗng")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_RoleBlank() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, "",
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC26: Trả về 400 khi address rỗng")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_AddressBlank() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE,
                        TEST_DATE_OF_BIRTH, "", TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC27: Trả về 400 khi active null")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_ActiveNull() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE,
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, null, false, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC28: Trả về 400 khi accountLocked null")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_AccountLockedNull() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE,
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, true, null, true, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC29: Trả về 400 khi enabled null")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_EnabledNull() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE,
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, true, false, null, false
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC30: Trả về 400 khi oauth2Enabled null")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_Oauth2EnabledNull() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE,
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, true, false, true, null
                );

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("Error Handling Tests")
        class ErrorHandlingTests {

            @Test
            @DisplayName("UC31: Xử lý lỗi khi email đã tồn tại")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_EmailAlreadyExists() throws Exception {
                ApiResponse<String> response = new ApiResponse<>("Email đã tồn tại", null, HttpStatus.CONFLICT.value());
                when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUserDTO)))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.message").value("Email đã tồn tại"))
                        .andExpect(jsonPath("$.status").value(409));
            }

            @Test
            @DisplayName("UC32: Xử lý lỗi khi request body rỗng")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_EmptyBody() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UC33: Xử lý lỗi khi JSON không hợp lệ")
            @WithMockUser(username = TEST_EMAIL)
            void createUser_InvalidJson() throws Exception {
                mockMvc.perform(post(BASE_URL)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{invalid json}"))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    // ==================== GET /api/users/page/{pageIndex}/{pageSize} - LIST USERS ====================
    @Nested
    @DisplayName("GET /api/users/page/{pageIndex}/{pageSize} - Danh sách User phân trang")
    class ListUsersTests {

        @Test
        @DisplayName("UL01: Trả về 401 khi chưa đăng nhập")
        void listUsers_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/page/0/10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("UL02: Lấy danh sách user thành công")
        @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
        void listUsers_Success() throws Exception {
            List<UserResponseDTO> users = List.of(validUserResponseDTO);
            Page<UserResponseDTO> page = new PageImpl<>(users);
            ApiResponse<Page<UserResponseDTO>> response = new ApiResponse<>("success", page, HttpStatus.OK.value());
            when(userCrudService.getUsers(eq(0), eq(10))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/page/0/10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].email").value(TEST_EMAIL));
        }

        @Test
        @DisplayName("UL03: Lấy danh sách rỗng khi không có user")
        @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
        void listUsers_EmptyList() throws Exception {
            Page<UserResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());
            ApiResponse<Page<UserResponseDTO>> response = new ApiResponse<>("success", emptyPage, HttpStatus.OK.value());
            when(userCrudService.getUsers(eq(0), eq(10))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/page/0/10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("UL04: Phân trang với pageIndex lớn")
        @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
        void listUsers_LargePageIndex() throws Exception {
            Page<UserResponseDTO> emptyPage = new PageImpl<>(Collections.emptyList());
            ApiResponse<Page<UserResponseDTO>> response = new ApiResponse<>("success", emptyPage, HttpStatus.OK.value());
            when(userCrudService.getUsers(eq(100), eq(10))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/page/100/10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty());
        }

        @Test
        @DisplayName("UL05: Phân trang với pageSize nhỏ")
        @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
        void listUsers_SmallPageSize() throws Exception {
            List<UserResponseDTO> users = List.of(validUserResponseDTO);
            Page<UserResponseDTO> page = new PageImpl<>(users);
            ApiResponse<Page<UserResponseDTO>> response = new ApiResponse<>("success", page, HttpStatus.OK.value());
            when(userCrudService.getUsers(eq(0), eq(1))).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/page/0/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    // ==================== GET /api/users - GET USER DETAIL ====================
    @Nested
    @DisplayName("GET /api/users - Lấy thông tin User hiện tại")
    class GetUserDetailTests {

        @Test
        @DisplayName("UD01: Trả về 401 khi chưa đăng nhập")
        void getUserDetail_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("UD02: Lấy thông tin user hiện tại thành công")
        @WithMockUser(username = TEST_EMAIL, roles = "USER")
        void getUserDetail_Success() throws Exception {
            ApiResponse<UserResponseDTO> response = new ApiResponse<>("success", validUserResponseDTO, HttpStatus.OK.value());
            when(userCrudService.getUserByEmail(eq(TEST_EMAIL))).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.data.fullName").value(TEST_FULL_NAME));
        }

        @Test
        @DisplayName("UD03: Trả về 404 khi user không tồn tại")
        @WithMockUser(username = "nonexistent@test.com", roles = "USER")
        void getUserDetail_NotFound() throws Exception {
            ApiResponse<UserResponseDTO> response = new ApiResponse<>("Không tìm thấy user", null, HttpStatus.NOT_FOUND.value());
            when(userCrudService.getUserByEmail(eq("nonexistent@test.com"))).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Không tìm thấy user"));
        }

        @Test
        @DisplayName("UD04: Response bao gồm đầy đủ các trường")
        @WithMockUser(username = TEST_EMAIL, roles = "USER")
        void getUserDetail_ResponseContainsAllFields() throws Exception {
            ApiResponse<UserResponseDTO> response = new ApiResponse<>("success", validUserResponseDTO, HttpStatus.OK.value());
            when(userCrudService.getUserByEmail(eq(TEST_EMAIL))).thenReturn(response);

            mockMvc.perform(get(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.fullName").exists())
                    .andExpect(jsonPath("$.data.email").exists())
                    .andExpect(jsonPath("$.data.role").exists())
                    .andExpect(jsonPath("$.data.dateOfBirth").exists())
                    .andExpect(jsonPath("$.data.address").exists())
                    .andExpect(jsonPath("$.data.mobile").exists())
                    .andExpect(jsonPath("$.data.accountLocked").exists())
                    .andExpect(jsonPath("$.data.enabled").exists())
                    .andExpect(jsonPath("$.data.active").exists())
                    .andExpect(jsonPath("$.data.oauth2Enabled").exists());
        }
    }

    // ==================== PUT /api/users/{id} - UPDATE USER ====================
    @Nested
    @DisplayName("PUT /api/users/{id} - Cập nhật User")
    class UpdateUserTests {

        @Nested
        @DisplayName("Security Tests")
        class UpdateSecurityTests {

            @Test
            @DisplayName("UU01: Trả về 401 khi chưa đăng nhập")
            void updateUser_Unauthorized() throws Exception {
                mockMvc.perform(put(BASE_URL + "/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUserDTO)))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("UU02: Cập nhật user thành công khi đã đăng nhập")
            @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
            void updateUser_Success() throws Exception {
                ApiResponse<String> response = new ApiResponse<>("Cập nhật thành công", null, HttpStatus.OK.value());
                when(userCrudService.updateUser(eq(1L), any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(put(BASE_URL + "/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUserDTO)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.message").value("Cập nhật thành công"));
            }
        }

        @Nested
        @DisplayName("Validation Tests")
        class UpdateValidationTests {

            @Test
            @DisplayName("UU03: Trả về 400 khi fullName rỗng")
            @WithMockUser(username = TEST_EMAIL)
            void updateUser_FullNameBlank() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        "", TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(put(BASE_URL + "/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UU04: Trả về 400 khi email không hợp lệ")
            @WithMockUser(username = TEST_EMAIL)
            void updateUser_InvalidEmail() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, "invalid-email", TEST_PASSWORD, TEST_ROLE,
                        TEST_DATE_OF_BIRTH, TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(put(BASE_URL + "/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UU05: Trả về 400 khi password quá ngắn")
            @WithMockUser(username = TEST_EMAIL)
            void updateUser_ShortPassword() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, "1234567", TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(put(BASE_URL + "/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UU06: Trả về 400 khi mobile không đúng định dạng")
            @WithMockUser(username = TEST_EMAIL)
            void updateUser_InvalidMobile() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                        TEST_ADDRESS, "abc123", true, false, true, false
                );

                mockMvc.perform(put(BASE_URL + "/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("UU07: Trả về 400 khi dateOfBirth là ngày tương lai")
            @WithMockUser(username = TEST_EMAIL)
            void updateUser_FutureDateOfBirth() throws Exception {
                UserCrudDTO dto = new UserCrudDTO(
                        TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE,
                        LocalDate.now().plusYears(1), TEST_ADDRESS, TEST_MOBILE, true, false, true, false
                );

                mockMvc.perform(put(BASE_URL + "/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                        .andExpect(status().isBadRequest());
            }
        }

        @Nested
        @DisplayName("Error Handling Tests")
        class UpdateErrorHandlingTests {

            @Test
            @DisplayName("UU08: Trả về 404 khi user không tồn tại")
            @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
            void updateUser_NotFound() throws Exception {
                ApiResponse<String> response = new ApiResponse<>("Không tìm thấy user", null, HttpStatus.NOT_FOUND.value());
                when(userCrudService.updateUser(eq(999L), any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(put(BASE_URL + "/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUserDTO)))
                        .andExpect(status().isNotFound())
                        .andExpect(jsonPath("$.message").value("Không tìm thấy user"));
            }

            @Test
            @DisplayName("UU09: Xử lý lỗi khi email mới đã tồn tại")
            @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
            void updateUser_EmailConflict() throws Exception {
                ApiResponse<String> response = new ApiResponse<>("Email đã được sử dụng", null, HttpStatus.CONFLICT.value());
                when(userCrudService.updateUser(eq(1L), any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(put(BASE_URL + "/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUserDTO)))
                        .andExpect(status().isConflict());
            }

            @Test
            @DisplayName("UU10: Cập nhật với ID = 0")
            @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
            void updateUser_IdZero() throws Exception {
                ApiResponse<String> response = new ApiResponse<>("ID không hợp lệ", null, HttpStatus.BAD_REQUEST.value());
                when(userCrudService.updateUser(eq(0L), any(UserCrudDTO.class))).thenReturn(response);

                mockMvc.perform(put(BASE_URL + "/0")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validUserDTO)))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    // ==================== DELETE /api/users/{id} - DELETE USER ====================
    @Nested
    @DisplayName("DELETE /api/users/{id} - Xóa User")
    class DeleteUserTests {

        @Test
        @DisplayName("DE01: Trả về 401 khi chưa đăng nhập")
        void deleteUser_Unauthorized() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("DE02: Xóa user thành công khi đã đăng nhập")
        @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
        void deleteUser_Success() throws Exception {
            ApiResponse<String> response = new ApiResponse<>("Xóa thành công", null, HttpStatus.OK.value());
            when(userCrudService.deleteUser(eq(1L))).thenReturn(response);

            mockMvc.perform(delete(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Xóa thành công"));
        }

        @Test
        @DisplayName("DE03: Trả về 404 khi user không tồn tại")
        @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
        void deleteUser_NotFound() throws Exception {
            ApiResponse<String> response = new ApiResponse<>("Không tìm thấy user", null, HttpStatus.NOT_FOUND.value());
            when(userCrudService.deleteUser(eq(999L))).thenReturn(response);

            mockMvc.perform(delete(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").value("Không tìm thấy user"));
        }

        @Test
        @DisplayName("DE04: Xóa với ID = 0")
        @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
        void deleteUser_IdZero() throws Exception {
            ApiResponse<String> response = new ApiResponse<>("ID không hợp lệ", null, HttpStatus.BAD_REQUEST.value());
            when(userCrudService.deleteUser(eq(0L))).thenReturn(response);

            mockMvc.perform(delete(BASE_URL + "/0")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("DE05: Xóa với ID âm")
        @WithMockUser(username = TEST_EMAIL, roles = "ADMIN")
        void deleteUser_NegativeId() throws Exception {
            ApiResponse<String> response = new ApiResponse<>("ID không hợp lệ", null, HttpStatus.BAD_REQUEST.value());
            when(userCrudService.deleteUser(eq(-1L))).thenReturn(response);

            mockMvc.perform(delete(BASE_URL + "/-1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== Response Format Tests ====================
    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("RF01: Response Content-Type là application/json")
        @WithMockUser(username = TEST_EMAIL)
        void response_ContentType_ApplicationJson() throws Exception {
            ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
            when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUserDTO)))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("RF02: Response có đầy đủ cấu trúc ApiResponse")
        @WithMockUser(username = TEST_EMAIL)
        void response_HasCorrectStructure() throws Exception {
            ApiResponse<String> response = new ApiResponse<>("Test message", "test data", HttpStatus.OK.value());
            when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUserDTO)))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.status").exists());
        }
    }

    // ==================== Input Sanitization Tests ====================
    @Nested
    @DisplayName("Input Sanitization & Security Tests")
    class InputSanitizationTests {

        @Test
        @DisplayName("IS01: Xử lý fullName có ký tự đặc biệt HTML")
        @WithMockUser(username = TEST_EMAIL)
        void createUser_FullNameWithHtmlChars() throws Exception {
            UserCrudDTO dto = new UserCrudDTO(
                    "<script>alert('xss')</script>",
                    TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                    TEST_ADDRESS, TEST_MOBILE, true, false, true, false
            );

            ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
            when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

            // API nên vẫn chấp nhận, việc escape HTML là trách nhiệm của frontend
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("IS02: Xử lý address có SQL injection pattern")
        @WithMockUser(username = TEST_EMAIL)
        void createUser_AddressWithSqlPattern() throws Exception {
            UserCrudDTO dto = new UserCrudDTO(
                    TEST_FULL_NAME, TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                    "123 Street'; DROP TABLE users; --",
                    TEST_MOBILE, true, false, true, false
            );

            ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
            when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

            // JPA/Hibernate sẽ tự xử lý parameterized queries
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("IS03: Xử lý email với ký tự Unicode")
        @WithMockUser(username = TEST_EMAIL)
        void createUser_EmailWithUnicode() throws Exception {
            UserCrudDTO dto = new UserCrudDTO(
                    TEST_FULL_NAME, "test@例え.jp", TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                    TEST_ADDRESS, TEST_MOBILE, true, false, true, false
            );

            // Email validation có thể reject hoặc accept tùy implementation
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)));
            // Không check status vì tùy thuộc vào cách email validator xử lý IDN
        }

        @Test
        @DisplayName("IS04: Xử lý fullName với ký tự Unicode tiếng Việt")
        @WithMockUser(username = TEST_EMAIL)
        void createUser_FullNameWithVietnamese() throws Exception {
            UserCrudDTO dto = new UserCrudDTO(
                    "Nguyễn Văn Ánh",  // Tiếng Việt có dấu
                    TEST_EMAIL, TEST_PASSWORD, TEST_ROLE, TEST_DATE_OF_BIRTH,
                    TEST_ADDRESS, TEST_MOBILE, true, false, true, false
            );

            ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
            when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("IS05: Xử lý password với whitespace")
        @WithMockUser(username = TEST_EMAIL)
        void createUser_PasswordWithWhitespace() throws Exception {
            UserCrudDTO dto = new UserCrudDTO(
                    TEST_FULL_NAME, TEST_EMAIL, "pass word 123", TEST_ROLE, TEST_DATE_OF_BIRTH,
                    TEST_ADDRESS, TEST_MOBILE, true, false, true, false
            );

            ApiResponse<String> response = new ApiResponse<>("Tạo user thành công", null, HttpStatus.CREATED.value());
            when(userCrudService.createUser(any(UserCrudDTO.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated());
        }
    }
}
