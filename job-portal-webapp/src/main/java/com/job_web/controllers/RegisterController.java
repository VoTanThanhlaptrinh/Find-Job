package com.job_web.controllers;

import java.security.Principal;

import javax.validation.Valid;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.job_web.data.UserRepository;
import com.job_web.dto.RegistationForm;
import com.job_web.models.User;
import com.job_web.service.IVerifyService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/register")
@Slf4j
public class RegisterController {
	private UserRepository repository;
	private PasswordEncoder encoder;
	private IVerifyService verifyService;

	public RegisterController(UserRepository repository, PasswordEncoder encoder) {
		super();
		this.repository = repository;
		this.encoder = encoder;
	}

	@GetMapping
	public String register(Principal principal, Model model) {
//		if (principal != null) {
//			model.addAttribute("user", repository.findByUsername(principal.getName()));
//			return "index";
//		}
		return "register";
	}

	@PostMapping
	public String redirectLogin(@Valid @ModelAttribute() RegistationForm registationForm, BindingResult bindingResult,
			Model model) {
		// TODO: process POST request
		if (bindingResult.hasErrors()) {
			model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
			log.info(bindingResult.getAllErrors().get(0).getDefaultMessage());
			return "register";
		}
		User user = registationForm.toUser(encoder);
//		if (repository.findByUsername(user.getUsername()) != null) {
//			model.addAttribute("error", "User: '" + user.getUsername() + "' đã tồn tại");
//			return "register";
//		}
		if (!registationForm.isPasswordMatch()) {
			model.addAttribute("error", "Xác nhận mật khẩu không trùng với mật khẩu");
			return "register";
		}
		log.info(registationForm.getUsername() + " Đăng ký thành công");
		repository.save(user);
		return "redirect:/login";
	}

}
