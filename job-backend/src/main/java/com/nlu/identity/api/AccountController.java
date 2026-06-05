package com.nlu.identity.api;

import com.nlu.identity.api.dto.ChangePassDTO;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.identity.api.dto.UserInfo;
import com.nlu.identity.domain.model.CurrentUser;
import com.nlu.identity.domain.model.User;
import com.nlu.identity.application.AccountService;
import com.nlu.shared.utils.MessageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class AccountController {
    private final AccountService accountService;

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<String>> updateProfile(@Valid @RequestBody UserInfo userInfo,
                                                             @CurrentUser User currentUser) {
        accountService.updateInfo(userInfo, currentUser);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("account.update.success"), null, HttpStatus.OK.value()));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserInfo>> getProfile(@CurrentUser User currentUser) {
        UserInfo data = accountService.getDetailUser(currentUser);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), data, HttpStatus.OK.value()));
    }

    @PutMapping("/password")
    public ResponseEntity<ApiResponse<String>> changePassword(@RequestBody ChangePassDTO changePassDTO,
                                                              @CurrentUser User currentUser) {
        accountService.changePassword(changePassDTO.getNewPass(), changePassDTO.getOldPass(), currentUser);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("message.success"), null, HttpStatus.OK.value()));
    }

    @GetMapping("/oauth2")
    public ResponseEntity<ApiResponse<String>> checkOauth2(@CurrentUser User currentUser) {
        boolean ok = accountService.checkOauth2(currentUser);
        if(!ok) {
            return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.oauth2.no_password"), currentUser.getEmail(), HttpStatus.OK.value()));
        }
        return ResponseEntity.status(301).body(new ApiResponse<>(MessageUtils.getMessage("auth.oauth2.no_issue"), currentUser.getEmail(), 301));
    }
}
