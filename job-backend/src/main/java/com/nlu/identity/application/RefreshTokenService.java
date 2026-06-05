package com.nlu.identity.application;

public interface RefreshTokenService {
	
	String createRefreshToken(String username);

	boolean isValid(String token);

	void deleteRefreshToken(String token);

	String reGenerateRefreshToken(String token);
}



