package com.job_web.identity.infrastructure.validation;


import com.job_web.identity.domain.repository.UserRepository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
@AllArgsConstructor
public class EmailExistValidator implements ConstraintValidator<EmailExist, String> {
	private UserRepository userRepository;
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		// TODO Auto-generated method stub
		return value == null || !userRepository.findByEmail(value).isPresent();
	}

}


