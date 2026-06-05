package com.nlu.identity.api;

import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.identity.domain.model.CurrentUser;
import com.nlu.identity.domain.model.User;
import com.nlu.shared.utils.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/account/roles", produces = "application/json")
@Slf4j
public class UserAccountController {

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<Object>> checkUser(@CurrentUser User currentUser) {
        if (currentUser != null) {
            log.info("check user login success");
            log.info(currentUser.getEmail());
            return ResponseEntity.status(HttpStatus.OK)
                    .body(new ApiResponse<>(MessageUtils.getMessage("message.success"), true, HttpStatus.OK.value()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiResponse<>(MessageUtils.getMessage("message.error"), false, HttpStatus.BAD_REQUEST.value()));
    }
}
