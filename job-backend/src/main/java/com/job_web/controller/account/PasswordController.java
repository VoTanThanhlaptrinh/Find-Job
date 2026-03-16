package com.job_web.controller.account;

import com.job_web.dto.auth.ChangePassDTO;
import com.job_web.dto.auth.ForgotPassDTO;
import com.job_web.dto.auth.ResetDTO;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.account.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/account", produces = "application/json")
@RequiredArgsConstructor
public class PasswordController {
    private final AccountService accountService;

    @GetMapping("/pub/code/{email}")
    public ResponseEntity<ApiResponse<String>> getCodeForgotPass(@PathVariable String email, HttpServletRequest request) {
        ApiResponse<String> res = accountService.sendCodeForgotPassword(request, email);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PutMapping("/pri/changePass")
    public ResponseEntity<ApiResponse<String>> changePass(@RequestBody ChangePassDTO changePassDTO) {
        ApiResponse<String> res = accountService.changePassword(changePassDTO.getNewPass(), changePassDTO.getOldPass());
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/pub/forgotPass")
    public ResponseEntity<ApiResponse<String>> forgotPass(@RequestBody @Valid ForgotPassDTO forgotPassDTO,
                                                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = accountService.forgotPassword(forgotPassDTO);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/pub/checkRandom/{random}")
    public ResponseEntity<ApiResponse<String>> checkRandom(@PathVariable String random) {
        ApiResponse<String> res = accountService.checkRandom(random);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PatchMapping("/pub/reset")
    public ResponseEntity<ApiResponse<String>> resetPass(@RequestBody @Valid ResetDTO resetDTO,
                                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = accountService.resetPassword(resetDTO);
        return ResponseEntity.status(res.getStatus()).body(res);
    }
}
