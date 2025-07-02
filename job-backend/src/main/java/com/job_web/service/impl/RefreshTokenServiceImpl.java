package com.job_web.service.impl;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.job_web.data.RefreshTokenRepository;
import com.job_web.data.UserRepository;
import com.job_web.models.RefreshToken;
import com.job_web.models.User;
import com.job_web.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	
	@Value("${application.service.impl.timeLimit}")
	private long timeLimit;
	@Override
	public RefreshToken createRefreshToken(String username) {
		Optional<User> user = userRepository.findByEmail(username);
		if (user.isPresent()) {
			RefreshToken refreshToken = RefreshToken.builder().userInfo(user.get()).token(UUID.randomUUID().toString())
					.expiryDate(Instant.now().plusSeconds(timeLimit)).build();
			return refreshTokenRepository.save(refreshToken);
		} else {
			throw new RuntimeException("username not found");
		}
	}

	@Override
	public Optional<RefreshToken> findByToken(String token) {
		return refreshTokenRepository.findByToken(token);
	}

	@Override
	public RefreshToken verifyExpiration(RefreshToken token) {
		if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
			refreshTokenRepository.delete(token);
			throw new RuntimeException(token.getToken() + " Refresh token is expired. Please make a new login..!");
		}
		return token;
	}

	@Override
	public void deleteRefreshToken(String token) {
		Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        refreshToken.ifPresent(refreshTokenRepository::delete);
	}

}
