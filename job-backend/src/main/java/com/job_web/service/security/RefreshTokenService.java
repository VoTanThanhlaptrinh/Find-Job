package com.job_web.service.security;

import java.util.Optional;

public interface RefreshTokenService {
	
	String createRefreshToken(String username);

	boolean isValid(String token);

	void deleteRefreshToken(String token);

	String reGenerateRefreshToken(String token);
}



