package com.job_web.identity.application.impl;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.job_web.identity.domain.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserRepositoryDetailsService implements UserDetailsService {
	@Autowired
	private UserRepository repository;

	@Override
	@Transactional
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		return repository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User with username: '" + email + "' not found"));
	}

}



