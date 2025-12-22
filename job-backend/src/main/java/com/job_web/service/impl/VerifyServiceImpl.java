package com.job_web.service.impl;

import java.time.Duration;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.job_web.service.IVerifyService;

@Service
@AllArgsConstructor
public class VerifyServiceImpl implements IVerifyService {
	/*
	 * không được khai báo như này vì cơ bản là Redis đã được cấu hình rồi
	 * nên @autoWired để Spring tìm phương thức khởi tạo cho Redis private final
	 * RedisTemplate<String, Object> redisTemplate;
	 * 
	 * public VerifyServiceIMP() { redisTemplate = new RedisTemplate<>(); }
	 */

	private final RedisTemplate<String, Object> redisTemplate;

	@Override
	public boolean containsKey(String key) {
		// TODO Auto-generated method stub
		return redisTemplate.opsForValue().get(key) != null;
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
