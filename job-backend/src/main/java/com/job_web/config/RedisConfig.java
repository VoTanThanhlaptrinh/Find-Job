package com.job_web.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
	@Value("${application.config.redis.host-name}")
	private String hostName;
	@Value("${application.config.redis.password}")
	private String password;
	@Value("${application.config.redis.port}")
	private int port;
	@Bean
	JedisConnectionFactory jedisConnectionFactory() {
		RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
//		redisStandaloneConfiguration.setHostName("localhost");
		redisStandaloneConfiguration.setHostName(hostName);
		// set password for deploy
//		redisStandaloneConfiguration.setPassword(password);
		redisStandaloneConfiguration.setPort(port);
		redisStandaloneConfiguration.setDatabase(0);

		JedisClientConfigurationBuilder jedisClientConfiguration = JedisClientConfiguration.builder();
		jedisClientConfiguration.connectTimeout(Duration.ofSeconds(60)); // Connection timeout
		jedisClientConfiguration.readTimeout(Duration.ofSeconds(5)); // Read timeout

		return new JedisConnectionFactory(redisStandaloneConfiguration, jedisClientConfiguration.build());
	}

	@Bean
	RedisTemplate<String, Object> redisTemplate() {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(jedisConnectionFactory());
		// Khi sử dụng RedisTemplate với String String thì có thể không cần cấu hình ở
		// dưới nhưng nếu là String Object hay một class nào thì phải cấu hình thêm.
		
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
}


