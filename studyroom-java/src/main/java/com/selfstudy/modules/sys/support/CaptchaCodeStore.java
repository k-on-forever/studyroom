package com.selfstudy.modules.sys.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 验证码存储：优先 Redis；{@code study.captcha.memory-fallback=true} 时在 Redis 异常或不可用时自动退回进程内缓存（单机开发可不依赖 Redis）。
 * 校验时与用户输入比对<strong>不区分大小写</strong>。
 */
@Slf4j
@Component
public class CaptchaCodeStore {

	private static final String PREFIX = "studyroom:captcha:";

	private final StringRedisTemplate stringRedisTemplate;

	@Value("${study.captcha.memory-fallback:true}")
	private boolean memoryFallback;

	private final ConcurrentHashMap<String, MemoryEntry> memory = new ConcurrentHashMap<>();

	public CaptchaCodeStore(StringRedisTemplate stringRedisTemplate) {
		this.stringRedisTemplate = stringRedisTemplate;
	}

	public static String redisKey(String uuid) {
		return PREFIX + uuid;
	}

	public void save(String uuid, String code, long ttlMinutes) {
		if (!StringUtils.hasText(uuid) || !StringUtils.hasText(code)) {
			return;
		}
		String key = redisKey(uuid.trim());
		boolean savedRedis = false;
		try {
			stringRedisTemplate.opsForValue().set(key, code, ttlMinutes, TimeUnit.MINUTES);
			savedRedis = true;
			memory.remove(key);
		} catch (DataAccessException ex) {
			log.warn("Captcha Redis SET failed, uuid prefix={}… — {}", uuid.substring(0, Math.min(4, uuid.length())), ex.getMessage());
		}
		if (!savedRedis && memoryFallback) {
			long expireAt = System.currentTimeMillis() + Math.max(1, ttlMinutes) * 60_000L;
			memory.put(key, new MemoryEntry(code, expireAt));
			log.debug("Captcha stored in memory for uuid");
		}
		if (!savedRedis && !memoryFallback) {
			throw new IllegalStateException("Redis unavailable and study.captcha.memory-fallback=false");
		}
	}

	public boolean peekMatches(String uuid, String userCaptcha) {
		if (!StringUtils.hasText(uuid) || !StringUtils.hasText(userCaptcha)) {
			return false;
		}
		String cached = getCached(redisKey(uuid.trim()));
		if (cached == null) {
			return false;
		}
		return captchaEquals(cached, userCaptcha);
	}

	public CaptchaOutcome verifyAndConsume(String uuid, String userCaptcha) {
		if (!StringUtils.hasText(uuid) || !StringUtils.hasText(userCaptcha)) {
			return CaptchaOutcome.MISMATCH;
		}
		String key = redisKey(uuid.trim());
		String cached = getCached(key);
		if (cached == null) {
			return CaptchaOutcome.MISSING_OR_EXPIRED;
		}
		if (!captchaEquals(cached, userCaptcha)) {
			deleteEverywhere(key);
			return CaptchaOutcome.MISMATCH;
		}
		deleteEverywhere(key);
		return CaptchaOutcome.OK;
	}

	private String getCached(String key) {
		try {
			String v = stringRedisTemplate.opsForValue().get(key);
			if (StringUtils.hasText(v)) {
				return v;
			}
		} catch (DataAccessException ex) {
			log.warn("Captcha Redis GET failed: {}", ex.getMessage());
		}
		return memoryGet(key);
	}

	private String memoryGet(String key) {
		if (!memoryFallback) {
			return null;
		}
		MemoryEntry e = memory.get(key);
		if (e == null) {
			return null;
		}
		if (System.currentTimeMillis() > e.expireAtMillis) {
			memory.remove(key);
			return null;
		}
		return e.code;
	}

	private static boolean captchaEquals(String stored, String userInput) {
		return stored.equalsIgnoreCase(userInput.trim());
	}

	private void deleteEverywhere(String key) {
		try {
			stringRedisTemplate.delete(key);
		} catch (DataAccessException ignored) {
			// ignore
		}
		memory.remove(key);
	}

	private record MemoryEntry(String code, long expireAtMillis) {
	}

	public enum CaptchaOutcome {
		OK,
		MISSING_OR_EXPIRED,
		MISMATCH
	}
}
