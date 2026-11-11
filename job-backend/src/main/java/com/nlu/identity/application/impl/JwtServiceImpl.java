package com.nlu.identity.application.impl;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import com.nlu.identity.application.JwtFamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import com.nlu.identity.application.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {
	@Value("${application.service.impl.secret-key}")
	private String secretkey;
	@Value("${application.service.impl.expiration}")
	private long jwtExpiration;
	private final JwtFamilyService jwtFamilyService;
	private final UserDetailsService userDetailsService;

	public String generateToken(UserDetails userDetails) {
		try{
			HashMap<String, Object> claims = new HashMap<>();
			List<String> roles = userDetails.getAuthorities()
					.stream()
					.map(GrantedAuthority::getAuthority)
					.collect(Collectors.toList());
			claims.put("roles", roles);
			return generateToken(claims, userDetails.getUsername(),jwtExpiration);
		}catch (Exception e){
			throw new RuntimeException(e);
		}
	}

	public String generateToken(String username) {
		// Load user to get roles for JWT
		UserDetails userDetails = userDetailsService.loadUserByUsername(username);
		return generateToken(userDetails);
	}

	public String extractUsername(String token) {
		return extractClaims(token, Claims::getSubject);
	}

	public <T> T extractClaims(String token, Function<Claims, T> claimResolver) {
		final Claims claims = extractAllClaims(token);
		return claimResolver.apply(claims);
	}

	public boolean isTokenValid(String token, UserDetails userDetails) {
		final Claims claims = extractAllClaims(token);
		final String username = claims.getSubject();
		final Object purpose = claims.get("purpose");
		if ("activate".equals(purpose)) {
			return false;
		}
		return username != null
				&& username.equals(userDetails.getUsername())
				&& isExpirationAfterNow(claims);
	}

	@Override
	public String generateActivationToken(String email) {
		HashMap<String, Object> claims = new HashMap<>();
		claims.put("purpose", "activate");
		return generateToken(claims, email, Duration.ofHours(24).toMillis());
	}

	@Override
	public boolean isActivationTokenValid(String token, String email) {
		try {
			final Claims claims = extractAllClaims(token);
			final String subject = claims.getSubject();
			final Object purpose = claims.get("purpose");
			return email != null
					&& email.equals(subject)
					&& "activate".equals(purpose)
					&& isExpirationAfterNow(claims);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public String extractJTI(String token) {
		return extractClaims(token, Claims::getId);
	}

	@Override
	public boolean isTokenExpired(String token) {
		return !extractExpiration(token).isAfter(nowUtc());
	}

	@Override
	public String generateRefreshToken(UserDetails user, long expirationMillis) {
		String familyId = UUID.randomUUID().toString();
		return helpGenerateRefreshToken(user.getUsername(), familyId, expirationMillis);
	}

	@Override
	public String generateRefreshToken(String username, String familyId, long expirationMillis) {
		return helpGenerateRefreshToken(username, familyId, expirationMillis);
	}

	private String helpGenerateRefreshToken(String username, String familyId, long expirationMillis){
		HashMap<String, Object> claims = new HashMap<>();
		claims.put("familyId", familyId);
		String token = generateToken(claims, username, expirationMillis);
		jwtFamilyService.saveFamilyJti(familyId, extractJTI(token));
		jwtFamilyService.saveUserFamily(username, familyId);
		return token;
	}

	@Override
	public String extractFamily(String token) {
		return extractAllClaims(token).get("familyId").toString();
	}

	private OffsetDateTime extractExpiration(String token) {
		Date expiration = extractClaims(token, Claims::getExpiration);
		return expiration != null ? expiration.toInstant().atOffset(ZoneOffset.UTC) : OffsetDateTime.MIN;
	}

	private boolean isExpirationAfterNow(Claims claims) {
		Date expiration = claims.getExpiration();
		if (expiration == null) {
			return false;
		}
		OffsetDateTime expirationTime = expiration.toInstant().atOffset(ZoneOffset.UTC);
		return expirationTime.isAfter(nowUtc());
	}

	private OffsetDateTime nowUtc() {
		return OffsetDateTime.now(ZoneOffset.UTC);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
	}

	private String generateToken(HashMap<String, Object> claims, String username, long expirationMillis) {
		return buildToken(claims, username, expirationMillis);
	}

	private String buildToken(HashMap<String, Object> claims, String username, long expirationMillis) {
		OffsetDateTime issuedAt = nowUtc();
		OffsetDateTime expiresAt = issuedAt.plus(expirationMillis, ChronoUnit.MILLIS);
		return Jwts.builder().claims(claims).subject(username)
				.issuedAt(Date.from(issuedAt.toInstant()))
				.id(UUID.randomUUID().toString())
				.expiration(Date.from(expiresAt.toInstant()))
				.signWith(getSignInKey()).compact();
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretkey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}



