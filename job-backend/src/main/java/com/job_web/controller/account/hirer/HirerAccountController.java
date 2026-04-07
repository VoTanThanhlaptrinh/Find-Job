package com.job_web.controller.account.hirer;

import com.job_web.dto.common.ApiResponse;
import com.job_web.models.CurrentUser;
import com.job_web.models.User;
import com.job_web.utills.MessageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/account/roles", produces = "application/json")
@Slf4j
public class HirerAccountController {

    @GetMapping("/hirer")
    public ResponseEntity<ApiResponse<Boolean>> checkHirerLogin(@CurrentUser User currentUser) {
        boolean res = currentUser != null;
        String mess = res ? MessageUtils.getMessage("message.success") : "";
        log.info(String.valueOf(res));
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(mess, res, HttpStatus.OK.value()));
    }
}
