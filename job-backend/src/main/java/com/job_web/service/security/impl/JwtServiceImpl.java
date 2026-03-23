package com.job_web.service.security.impl;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import com.job_web.models.User;
import com.job_web.service.security.JwtFamilyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.job_web.service.security.JwtService;

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
	public String generateToken(UserDetails userDetails) {
		try{
			HashMap<String, Object> claims = new HashMap<>();
			List<String> roles = userDetails.getAuthorities()
					.stream()
					.map(GrantedAuthority::getAuthority)
					.collect(Collectors.toList());
			claims.put("roles", roles);
			return gerenateToken(claims, userDetails.getUsername());
		}catch (Exception e){
			throw new RuntimeException(e);
		}
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
	public String extractJTI(String token) {
		return extractClaims(token, Claims::getId);
	}

	@Override
	public boolean isTokenExpired(String token) {
		// TODO Auto-generated method stub
		return extractExpiration(token).before(new Date());
	}

	@Override
	public String generateRefreshToken(UserDetails user) {
		String familyId = UUID.randomUUID().toString();
		return helpGenerateRefreshToken(user.getUsername(), familyId);
	}

	@Override
	public String generateRefreshToken(String username, String familyId) {
		return helpGenerateRefreshToken(username, familyId);
	}

	private String helpGenerateRefreshToken(String username, String familyId){
		HashMap<String, Object> claims = new HashMap<>();
		claims.put("familyId", familyId);
		String token = gerenateToken(claims, username);
		jwtFamilyService.saveFamilyJti(familyId,extractJTI(token));
		return token;
	}

	@Override
	public String extractFamily(String token) {
		return extractAllClaims(token).get("familyId").toString();
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
				.id(UUID.randomUUID().toString())
				.expiration(new Date(System.currentTimeMillis()+ jwtExpiration))
				.signWith(getSignInKey()).compact();
	}

	private SecretKey getSignInKey() {
		byte[] keyBytes = Decoders.BASE64.decode(secretkey);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}



