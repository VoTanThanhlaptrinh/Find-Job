package com.nlu.shared.infrastructure.config;

import java.time.Duration;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
	@Value("${application.config.redis.host-name}")
	private String hostName;
	@Value("${application.config.redis.password}")
	private String password;
	@Value("${application.config.redis.port}")
	private int port;
	@Value("${application.config.redis.username}")
	private String username;

	@Bean
	JedisConnectionFactory jedisConnectionFactory() {
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
		redisStandaloneConfiguration.setHostName(hostName);
		if (username != null && !username.isBlank()) {
			redisStandaloneConfiguration.setUsername(username);
			redisStandaloneConfiguration.setPassword(password);
		}
		redisStandaloneConfiguration.setPort(port);
		redisStandaloneConfiguration.setDatabase(0);

		GenericObjectPoolConfig<Object> poolConfig = new GenericObjectPoolConfig<>();
		poolConfig.setMaxTotal(32);
		poolConfig.setMaxIdle(16);
		poolConfig.setMinIdle(4);
		poolConfig.setTestOnBorrow(false);
		poolConfig.setTestWhileIdle(true);
		poolConfig.setMaxWait(Duration.ofSeconds(5));

		JedisClientConfigurationBuilder jedisClientConfiguration = JedisClientConfiguration.builder();
		jedisClientConfiguration.connectTimeout(Duration.ofSeconds(60)); // Connection timeout
		jedisClientConfiguration.readTimeout(Duration.ofSeconds(5)); // Read timeout
		jedisClientConfiguration.usePooling().poolConfig(poolConfig);

		return new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration.build());
	}

	@Bean
	RedisTemplate<String, Object> redisTemplate() {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(jedisConnectionFactory());
		
		// Sử dụng StringRedisSerializer cho key và hashKey để tránh null serializer
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringRedisSerializer);
		template.setHashKeySerializer(stringRedisSerializer);

		// Sử dụng GenericJackson2JsonRedisSerializer để lưu object dưới dạng JSON
		template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
		template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

		template.afterPropertiesSet();
		return template;
	}

	@Bean
	StringRedisTemplate stringRedisTemplate() {
		return new StringRedisTemplate(jedisConnectionFactory());
	}

	@Bean(name = "cacheByteRedisTemplate")
	RedisTemplate<String, byte[]> cacheByteRedisTemplate() {
		RedisTemplate<String, byte[]> template = new RedisTemplate<>();
		template.setConnectionFactory(jedisConnectionFactory());

		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringRedisSerializer);
		template.setHashKeySerializer(stringRedisSerializer);
		template.setValueSerializer(RedisSerializer.byteArray());
		template.setHashValueSerializer(RedisSerializer.byteArray());

		template.afterPropertiesSet();
		return template;
	}
}


