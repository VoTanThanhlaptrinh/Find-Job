package com.job_web.service.security;

import java.util.function.Function;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;

public interface JwtService {
	 String generateToken(UserDetails user);
	 String generateToken(String username);
	 String extractUsername(String token);

	 <T> T extractClaims(String token, Function<Claims, T> claimResolver);

	 boolean isTokenValid(String token, UserDetails userDetails);
	 String extractJTI(String token);
	 boolean isTokenExpired(String token);
	 String generateRefreshToken(UserDetails user, long expirationMillis);
	String generateRefreshToken(String username, String familyId, long expirationMillis);

	String extractFamily(String token);
}



