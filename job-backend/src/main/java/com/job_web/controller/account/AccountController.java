package com.job_web.controller.account;

import com.job_web.dto.auth.ChangePassDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.profile.UserInfo;
import com.job_web.models.CurrentUser;
import com.job_web.models.User;
import com.job_web.service.account.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/account", produces = "application/json")
@RequiredArgsConstructor
@Slf4j
public class AccountController {
    private final AccountService accountService;

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<String>> updateProfile(@Valid @RequestBody UserInfo userInfo,
                                                             @CurrentUser User currentUser) {
        accountService.updateInfo(userInfo, currentUser);
        return ResponseEntity.ok(new ApiResponse<>("Cập nhật thành công", null, HttpStatus.OK.value()));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserInfo>> getProfile(@CurrentUser User currentUser) {
        UserInfo data = accountService.getDetailUser(currentUser);
        return ResponseEntity.ok(new ApiResponse<>("success", data, HttpStatus.OK.value()));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePassDTO changePassDTO,
                                                              @CurrentUser User currentUser) {
        accountService.changePassword(changePassDTO.getNewPass(), changePassDTO.getOldPass(), currentUser);
        return ResponseEntity.ok(new ApiResponse<>("success", null, HttpStatus.OK.value()));
    }

    @GetMapping("/roles/user")
    public ResponseEntity<ApiResponse<Object>> checkUser(@CurrentUser User currentUser) {
        if (currentUser != null) {
            log.info("check user login success");
            log.info(currentUser.getEmail());
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>("success", true, HttpStatus.OK.value()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>("error", false, HttpStatus.BAD_REQUEST.value()));
    }

    @GetMapping("/roles/hirer")
    public ResponseEntity<ApiResponse<Boolean>> checkHirerLogin(@CurrentUser User currentUser) {
        boolean res = currentUser != null;
        String mess = res ? "success" : "";
        log.info(String.valueOf(res));
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(mess, res, HttpStatus.OK.value()));
    }

    @GetMapping("/oauth2")
    public ResponseEntity<ApiResponse<String>> checkOauth2(@CurrentUser User currentUser) {
        boolean ok = accountService.checkOauth2(currentUser);
        if(!ok) {
            return ResponseEntity.ok(new ApiResponse<>("Tài khoản của bạn chưa có mật khẩu, bạn cần xác thực để tạo mới", currentUser.getEmail(), HttpStatus.OK.value()));
        }
        return ResponseEntity.status(301).body(new ApiResponse<>("không có vấn đề", currentUser.getEmail(), 301));
    }
}
