package com.example;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin
public class MessageController {
	private final RedisTemplate<String, Message> redisTemplate;

	public MessageController(RedisTemplate<String, Message> redisTemplate) {
		this.redisTemplate = redisTemplate;
	}

	@GetMapping(path = "/messages/{key}")
	public Message getMessage(@PathVariable String key) {
		return this.redisTemplate.opsForValue().get(buildRedisKey(key));
	}

	@PostMapping(path = "/messages/{key}")
	public void setMessage(@PathVariable String key, @RequestBody MessageRequest request) {
		final Message message = new Message(key, request.text());
		this.redisTemplate.opsForValue().set(buildRedisKey(key), message);
	}

	static String buildRedisKey(String key) {
		return "message_%s".formatted(key);
	}

	record MessageRequest(String text) {}

	record Message(String key, String text) {}
}
