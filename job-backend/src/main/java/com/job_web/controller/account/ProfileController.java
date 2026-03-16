package com.job_web.controller.account;

import java.security.Principal;

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
public class ProfileController {
    private final AccountService accountService;

    @PutMapping("/pri/updateUserInfo")
    public ResponseEntity<ApiResponse<String>> putUpdateUserInfo(@Valid @RequestBody UserInfo userInfo,
                                                                 BindingResult bindingResult,
                                                                 Principal principal) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = accountService.updateInfo(userInfo, principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/pri/detail")
    public ResponseEntity<ApiResponse<UserInfo>> getDetails(Principal principal) {
        ApiResponse<UserInfo> res = accountService.getDetailUser(principal);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/pri/u/isUser")
    public ResponseEntity<ApiResponse<Object>> checkUser(Principal principal) {
        if (principal != null) {
            log.info("check user login success");
            log.info(principal.getName());
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>("success", true, HttpStatus.OK.value()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>("error", false, HttpStatus.BAD_REQUEST.value()));
    }

    @GetMapping("/pri/h/isHirer")
    public ResponseEntity<ApiResponse<Boolean>> checkHirerLogin(Principal principal) {
        boolean res = principal != null;
        String mess = res ? "success" : "Báº¡n chÆ°a Ä‘Äƒng nháº­p";
        log.info(String.valueOf(res));
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(mess, res, HttpStatus.OK.value()));
    }

    @GetMapping("/pri/checkLogin")
    public ResponseEntity<ApiResponse<Boolean>> checkLogin(Principal principal) {
        boolean res = principal != null;
        String mess = res ? "success" : "Báº¡n chÆ°a Ä‘Äƒng nháº­p";
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(mess, res, HttpStatus.OK.value()));
    }
}
