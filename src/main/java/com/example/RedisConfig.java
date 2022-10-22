package com.example;

import com.example.MessageController.Message;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration(proxyBeanMethods = false)
public class RedisConfig {

	@Bean
	public RedisTemplate<String, Message> messageRedisTemplate(RedisConnectionFactory redisConnectionFactory) {
		RedisTemplate<String, Message> redisTemplate = new RedisTemplate<>();
		redisTemplate.setConnectionFactory(redisConnectionFactory);
		redisTemplate.setKeySerializer(new StringRedisSerializer());
		redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Message.class));
		return redisTemplate;
	}
}
