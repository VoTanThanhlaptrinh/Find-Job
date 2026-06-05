package com.nlu.identity.api;

import com.nlu.identity.api.dto.LoginDTO;
import com.nlu.identity.api.dto.RegistrationForm;
import com.nlu.shared.domain.model.ApiResponse;
import com.nlu.identity.application.AuthService;
import com.nlu.shared.utils.MessageUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/auth/user", produces = "application/json")
@RequiredArgsConstructor
public class UserAuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@RequestBody @Valid LoginDTO login,
                                                     HttpServletRequest request,
                                                     HttpServletResponse response) {
        String accessToken = authService.loginUser(login, request, response);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.login.success"), accessToken, HttpStatus.OK.value()));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@RequestBody @Valid RegistrationForm registrationForm) {
        String username = authService.registerUser(registrationForm);
        return ResponseEntity.ok(new ApiResponse<>(MessageUtils.getMessage("auth.register.success"), username, HttpStatus.OK.value()));
    }
}
