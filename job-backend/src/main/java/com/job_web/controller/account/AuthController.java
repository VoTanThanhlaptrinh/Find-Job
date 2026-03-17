package com.job_web.controller.account;

import java.util.concurrent.CompletableFuture;

import com.job_web.dto.auth.LoginDTO;
import com.job_web.dto.auth.RegistationForm;
import com.job_web.dto.common.ApiResponse;
import com.job_web.service.account.AccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping(path = "/api/account", produces = "application/json")
@RequiredArgsConstructor
public class AuthController {
    private final AccountService accountService;

    @PostMapping("/pub/u/login")
    public ResponseEntity<ApiResponse<String>> userLogin(@RequestBody LoginDTO login,
                                                         HttpServletRequest request,
                                                         HttpServletResponse response,
                                                         BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = accountService.login(login, request, response);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/pub/u/register")
    public ResponseEntity<ApiResponse<String>> userRegister(@RequestBody @Valid RegistationForm registationForm,
                                                            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = accountService.register(registationForm);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/pub/h/login")
    public ResponseEntity<ApiResponse<String>> hirerLogin(@RequestBody LoginDTO login,
                                                          HttpServletRequest request,
                                                          HttpServletResponse response,
                                                          BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = accountService.login(login, request, response);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @PostMapping("/pub/h/register")
    public ResponseEntity<ApiResponse<String>> hirerRegister(@RequestBody @Valid RegistationForm registationForm,
                                                             BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(bindingResult.getAllErrors().get(0).getDefaultMessage(), null, 400));
        }
        ApiResponse<String> res = accountService.register(registationForm);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @Async
    @GetMapping("/pri/sendLink/{email}")
    public CompletableFuture<ResponseEntity<ApiResponse<String>>> getLinkVerify(@PathVariable String email) {
        ApiResponse<String> res = accountService.sendLinkActivate(email);
        return CompletableFuture.completedFuture(ResponseEntity.status(res.getStatus()).body(res));
    }

    @GetMapping("/pub/activate/{token}")
    public ResponseEntity<ApiResponse<String>> activateAccount(@PathVariable String token) {
        ApiResponse<String> res = accountService.activeAccount(token);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/pub/refreshToken")
    public ResponseEntity<ApiResponse<String>> getRefreshToken(HttpServletRequest request, HttpServletResponse response) {
        ApiResponse<String> res = accountService.refreshToken(request, response);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/pub/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request, HttpServletResponse response) {
        ApiResponse<String> res = accountService.logout(request, response);
        return ResponseEntity.status(res.getStatus()).body(res);
    }

    @GetMapping("/pub/url/google")
    public ResponseEntity<ApiResponse<String>> googleUrl(HttpServletRequest req) {
        String base = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String url = base + "/oauth2/authorization/google";
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>("success", url, HttpStatus.OK.value()));
    }

    @GetMapping("/pri/checkOauth2")
    public ResponseEntity<ApiResponse<Boolean>> checkOauth2(java.security.Principal principal) {
        boolean res = principal != null;
        String mess = res ? "success" : "";
        return ResponseEntity.status(HttpStatus.OK).body(new ApiResponse<>(mess, res, HttpStatus.OK.value()));
    }
}
