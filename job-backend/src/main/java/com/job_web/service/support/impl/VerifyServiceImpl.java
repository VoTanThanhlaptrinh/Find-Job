package com.job_web.service.support.impl;

import java.time.Duration;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.job_web.service.support.VerificationService;

@Service
@AllArgsConstructor
public class VerifyServiceImpl implements VerificationService {

	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	public boolean containsKey(String key) {
		// TODO Auto-generated method stub
		return redisTemplate.opsForValue().get("recovery:" + key) != null;
	}

	@Override
	public void add(String key, String value) {
		// TODO Auto-generated method stub
		redisTemplate.opsForValue().set(key, value);
	}

	@Override
	public void add(String key, String ref, long secondTimeout) {
		// TODO Auto-generated method stub
		redisTemplate.opsForValue().set(key, ref, Duration.ofSeconds(secondTimeout));
	}

	@Override
	public Object getValue(String key) {
		// TODO Auto-generated method stub
		return redisTemplate.opsForValue().get(key);
	}

	@Override
	public void delete(String key) {
		// TODO Auto-generated method stub
		redisTemplate.delete(key);
	}

}



