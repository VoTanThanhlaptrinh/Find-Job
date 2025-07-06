package com.job_web.service.impl;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.job_web.service.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtServiceImpl implements JwtService {
	@Value("${application.service.impl.secret-key}")
	private String secretkey;
	@Value("${application.service.impl.expiration}")
	private long jwtExpiration;

	public String generateToken(UserDetails userDetails) {
		HashMap<String, Object> claims = new HashMap<>();
		List<String> roles = userDetails.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.collect(Collectors.toList());
		claims.put("roles", roles);
		return gerenateToken(claims, userDetails.getUsername());
	}
	public String generateToken(String username) {
		return gerenateToken(new HashMap<>(), username);
	}

	public String extractUsername(String token) {
		return extractClaims(token, Claims::getSubject);
	}

	public <T> T extractClaims(String token, Function<Claims, T> claimResolver) {
		final Claims claims = extractAllClaims(token);
		return claimResolver.apply(claims);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final String username = extractUsername(token);
		return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
	}
	@Override
	public boolean isTokenExpired(String token) {
		// TODO Auto-generated method stub
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		// TODO Auto-generated method stub
		return extractClaims(token, Claims::getExpiration);
	}

	private Claims extractAllClaims(String token) {
		// TODO Auto-generated method stub
		return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
	}

	private String gerenateToken(HashMap<String, Object> claims, String username) {
		// TODO Auto-generated method stub
		return buildToken(claims, username);
	}

	private String buildToken(HashMap<String, Object> claims,  String username) {
		// TODO Auto-generated method stub
		return Jwts.builder().claims(claims).subject(username)
				.issuedAt(new Date(System.currentTimeMillis()))// set thời điểm tạo ra
				.expiration(new Date(System.currentTimeMillis()+ jwtExpiration))
				.signWith(getSignInKey()).compact();
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretkey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
