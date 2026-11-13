package com.nlu.identity.application.impl;



import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.nlu.identity.domain.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRepositoryDetailsService implements UserDetailsService {
	private final UserRepository repository;

	@Override
	@Transactional(readOnly = true)
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		long startNs = System.nanoTime();
		UserDetails userDetails = repository.findByEmail_Value(email)
				.orElseThrow(() -> new UsernameNotFoundException("User with username: '" + email + "' not found"));
		long dbQueryMs = (System.nanoTime() - startNs) / 1_000_000L;
		log.info("[PERF][Service:DB][UserRepositoryDetailsService] loadUserByUsername email={} | dbQuery={}ms",
				email, dbQueryMs);
		return userDetails;
	}

}



