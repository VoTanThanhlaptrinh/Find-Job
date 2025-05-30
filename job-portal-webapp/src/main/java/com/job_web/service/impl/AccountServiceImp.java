package com.job_web.service.impl;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.job_web.service.AccountService;
@Service
public class AccountServiceImp implements AccountService {
	private PasswordEncoder encoder;

	public AccountServiceImp(PasswordEncoder encoder) {
		super();
		this.encoder = encoder;
	}

	@Override
	public boolean checkPassword( String passwordInput, String passwordInstored) {
		// TODO Auto-generated method stub
		return encoder.matches(passwordInput, passwordInstored);
	}

}
