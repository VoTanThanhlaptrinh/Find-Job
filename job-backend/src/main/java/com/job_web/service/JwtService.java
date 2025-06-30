package com.job_web.service;

import java.util.function.Function;

import org.springframework.security.core.userdetails.UserDetails;

import io.jsonwebtoken.Claims;

public interface JwtService {
	public String generateToken(String username);

	public String extractUsername(String token);

	public <T> T extractClaims(String token, Function<Claims, T> claimResolver);

	public boolean isTokenValid(String token, UserDetails userDetails);
	
	public boolean isTokenExpired(String token);
}
