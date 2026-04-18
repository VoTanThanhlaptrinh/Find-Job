package com.job_web.service.security.impl;

import java.time.Duration;
import java.util.Optional;

import com.job_web.service.security.JwtFamilyService;
import com.job_web.service.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.job_web.data.UserRepository;
import com.job_web.models.User;
import com.job_web.service.security.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@Slf4j
@RequiredArgsConstructor
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final JwtFamilyService jwtFamilyService;
	@Value("${application.service.impl.timeLimit}")
	private long refreshTokenTtlSeconds;

	@Override
	public String createRefreshToken(String username) {
		// Success path — no INFO logging (high traffic, covered by Controller logs).
		Optional<User> user = userRepository.findByEmail(username);
		if (user.isPresent()) {
			return jwtService.generateRefreshToken(user.get(), Duration.ofSeconds(refreshTokenTtlSeconds).toMillis());
		} else {
			log.warn("Refresh token creation failed — user not found");
			throw new RuntimeException("username not found");
		}
	}

	@Override
	public boolean isValid(String token) {
		// Success path — silent. Only log failures.
		try {
			String jti = jwtService.extractJTI(token);
			String familyId = jwtService.extractFamily(token);
			boolean valid = familyId != null && jwtFamilyService.getFamilyJti(familyId).equals(jti);
			if (!valid) {
				// WARN: possible token reuse or tampering — the JTI no longer
				// matches the latest one stored for this family. This could
				// indicate a stolen refresh token being replayed.
				log.warn("Refresh token JTI mismatch — possible token reuse detected");
			}
			return valid;
		} catch (Exception e) {
			log.warn("Refresh token validation failed — token expired or malformed");
			return false;
		}
	}

	@Override
	public void deleteRefreshToken(String token) {
		try {
			String familyId = jwtService.extractFamily(token);
			jwtFamilyService.deleteFamily(familyId);
			// No INFO — logout success is already logged by AuthServiceImpl.
		} catch (Exception e) {
			log.warn("Refresh token revocation failed — could not extract family");
			throw new RuntimeException(e);
		}
	}

	@Override
	public String reGenerateRefreshToken(String token) {
		try {
			String familyId = jwtService.extractFamily(token);
			String username = jwtService.extractUsername(token);
			return jwtService.generateRefreshToken(username, familyId, Duration.ofSeconds(refreshTokenTtlSeconds).toMillis());
			// No INFO — rotation success is already logged by AuthServiceImpl.
		} catch (Exception e) {
			log.warn("Refresh token rotation failed — token expired or malformed");
			throw new RuntimeException(e);
		}
	}
}
