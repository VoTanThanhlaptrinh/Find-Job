package com.job_web.identity.application;

public interface RefreshTokenService {
	
	String createRefreshToken(String username);

	boolean isValid(String token);

	void deleteRefreshToken(String token);

	String reGenerateRefreshToken(String token);
}



