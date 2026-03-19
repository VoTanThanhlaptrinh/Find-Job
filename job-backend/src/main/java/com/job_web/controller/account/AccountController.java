package com.job_web.controller.account;

import java.security.Principal;

import com.job_web.dto.auth.ChangePassDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.dto.profile.UserInfo;
import com.job_web.service.account.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
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
                                                             BindingResult bindingResult,
                                                             Principal principal) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = accountService.updateInfo(userInfo, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserInfo>> getProfile(Principal principal) {
        ApiResponse<UserInfo> res = accountService.getDetailUser(principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePassDTO changePassDTO) {
        ApiResponse<String> res = accountService.changePassword(changePassDTO.getNewPass(), changePassDTO.getOldPass());
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/roles/user")
    public ResponseEntity<ApiResponse<Object>> checkUser(Principal principal) {
        if (principal != null) {
            log.info("check user login success");
            log.info(principal.getName());
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>("success", true, HttpStatus.OK.value()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>("error", false, HttpStatus.BAD_REQUEST.value()));
    }

    @GetMapping("/roles/hirer")
    public ResponseEntity<ApiResponse<Boolean>> checkHirerLogin(Principal principal) {
        boolean res = principal != null;
        String mess = res ? "success" : "";
        log.info(String.valueOf(res));
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(mess, res, HttpStatus.OK.value()));
    }

    @GetMapping("/oauth2")
    public ResponseEntity<ApiResponse<String>> checkOauth2(Principal principal) {
        ApiResponse<String> res = accountService.checkOauth2(principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
