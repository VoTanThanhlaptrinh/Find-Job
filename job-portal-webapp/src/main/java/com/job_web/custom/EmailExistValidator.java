package com.job_web.custom;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.job_web.data.UserRepository;

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
