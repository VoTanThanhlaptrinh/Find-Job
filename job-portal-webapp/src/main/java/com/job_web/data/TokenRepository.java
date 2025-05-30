package com.job_web.data;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.job_web.models.Token;

public interface TokenRepository extends JpaRepository<Token, Long> {
	Optional<Token> findByToken(String token);
}
