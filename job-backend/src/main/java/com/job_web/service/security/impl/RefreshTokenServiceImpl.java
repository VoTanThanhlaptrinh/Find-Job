package com.job_web.service.security.impl;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.job_web.service.security.JwtFamilyService;
import com.job_web.service.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.job_web.data.UserRepository;
import com.job_web.models.User;
import com.job_web.service.security.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final JwtFamilyService jwtFamilyService;
	@Value("${application.service.impl.timeLimit}")
	private long timeLimit;
	@Override
	public String createRefreshToken(String username) {
		Optional<User> user = userRepository.findByEmail(username);
		if (user.isPresent()) {
			jwtService.generateRefreshToken(user.get());
			return null;
		} else {
			throw new RuntimeException("username not found");
		}
	}

	@Override
	public boolean isValid(String token) {
		try {
			String jti = jwtService.extractJTI(token);
			String familyId = jwtService.extractFamily(token);
			return familyId != null && jwtFamilyService.getFamilyJti(familyId).equals(jti);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void deleteRefreshToken(String token) {
		try {
			String familyId = jwtService.extractFamily(token);
			jwtFamilyService.deleteFamily(familyId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String reGenerateRefreshToken(String token) {
		try {
			String familyId = jwtService.extractFamily(token);
			String username = jwtService.extractUsername(token);
			return jwtService.generateRefreshToken(username,familyId);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}



