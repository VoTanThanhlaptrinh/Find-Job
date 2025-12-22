package com.job_web.service;

import java.util.Optional;

import com.job_web.models.RefreshToken;

public interface RefreshTokenService {
	
	RefreshToken createRefreshToken(String username);

	Optional<RefreshToken> findByToken(String token);
	
	RefreshToken verifyExpiration(RefreshToken token);

	void deleteRefreshToken(String token);
}
