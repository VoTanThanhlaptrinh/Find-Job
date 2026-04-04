package web_application.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.job_web.controller.account.AccountController;
import com.job_web.dto.auth.ChangePassDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.profile.UserInfo;
import com.job_web.service.account.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import web_application.support.TestSecurityConfig;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc
@Import(TestSecurityConfig.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    private static final String BASE_URL = "/api/account";
    private static final String TEST_EMAIL = "user@test.com";

    private UserInfo validUserInfo;

    @BeforeEach
    void setUp() {
        objectMapper.registerModule(new JavaTimeModule());
        validUserInfo = new UserInfo(
                "Nguyen Van A",
                LocalDate.of(1995, 5, 15),
                "123 Le Loi, HCM",
                "0123456789"
        );
    }

    @Nested
    @DisplayName("GET /api/account/profile - Lay thong tin tai khoan")
    class GetProfileTests {

        @Test
        @DisplayName("AC01: Tra ve 401 khi chua dang nhap")
        void getProfile_Unauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/profile"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("AC02: Lay profile thanh cong khi da dang nhap")
        @WithMockUser(username = TEST_EMAIL, roles = "USER")
        void getProfile_Success() throws Exception {
            ApiResponse<UserInfo> response = new ApiResponse<>("success", validUserInfo, HttpStatus.OK.value());
            when(accountService.getDetailUser(any())).thenReturn(response);

            mockMvc.perform(get(BASE_URL + "/profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"))
                    .andExpect(jsonPath("$.data.mobile").value("0123456789"));
        }
    }

    @Nested
    @DisplayName("PUT /api/account/profile - Cap nhat tai khoan")
    class UpdateProfileTests {

        @Test
        @DisplayName("AC03: Cap nhat profile thanh cong")
        @WithMockUser(username = TEST_EMAIL, roles = "USER")
        void updateProfile_Success() throws Exception {
            ApiResponse<String> response = new ApiResponse<>("Cap nhat thanh cong", null, HttpStatus.OK.value());
            when(accountService.updateInfo(any(UserInfo.class), any())).thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUserInfo)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Cap nhat thanh cong"));
        }

        @Test
        @DisplayName("AC04: Tra ve 400 khi mobile khong dung dinh dang")
        @WithMockUser(username = TEST_EMAIL, roles = "USER")
        void updateProfile_InvalidMobile() throws Exception {
            UserInfo invalidUserInfo = new UserInfo(
                    "Nguyen Van A",
                    LocalDate.of(1995, 5, 15),
                    "123 Le Loi, HCM",
                    "abc123"
            );

            mockMvc.perform(put(BASE_URL + "/profile")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidUserInfo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    @Nested
    @DisplayName("Security Tests")
    class SecurityTests {

        @Test
        @DisplayName("AC05: Endpoint /roles/user yeu cau dung quyen USER")
        @WithMockUser(username = "hirer@test.com", roles = "HIRER")
        void checkUserRole_ForbiddenForHirer() throws Exception {
            mockMvc.perform(get(BASE_URL + "/roles/user"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("AC06: Doi mat khau thanh cong khi da xac thuc")
        @WithMockUser(username = TEST_EMAIL, roles = "USER")
        void changePassword_Success() throws Exception {
            ChangePassDTO dto = new ChangePassDTO("oldPassword1", "newPassword1", "newPassword1");
            ApiResponse<String> response = new ApiResponse<>("Doi mat khau thanh cong", null, HttpStatus.OK.value());
            when(accountService.changePassword("newPassword1", "oldPassword1")).thenReturn(response);

            mockMvc.perform(put(BASE_URL + "/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Doi mat khau thanh cong"));
        }
    }
}
