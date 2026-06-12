package com.selfstudy.modules.applet.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 注册短信验证码（模拟）：仅进程内存储，不依赖 Redis，避免本机 Redis 不可连时发送失败。
 */
@Slf4j
@Component
public class AppletRegisterSmsCodeStore {

	private final ConcurrentHashMap<String, MemoryEntry> codes = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Long> lastSendAt = new ConcurrentHashMap<>();

	private static String normKey(String account) {
		return account.trim().toLowerCase();
	}

	/** 注册验证码 */
	public String saveNewCode(String account, long ttlMinutes) {
		return saveNewCode(account, ttlMinutes, "reg");
	}

	/** @param purpose 业务前缀，如 reg、pwd_rst，避免不同场景验证码串用 */
	public String saveNewCode(String account, long ttlMinutes, String purpose) {
		if (!StringUtils.hasText(account)) {
			return null;
		}
		String p = StringUtils.hasText(purpose) ? purpose.trim() : "reg";
		String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
		String k = normKey(account) + ":" + p;
		long expireAt = System.currentTimeMillis() + Math.max(1, ttlMinutes) * 60_000L;
		codes.put(k, new MemoryEntry(code, expireAt));
		log.info("SMS(模拟) [{}] 验证码 account={} code={}", p, mask(account), code);
		return code;
	}

	public boolean verifyAndConsume(String account, String userCode) {
		return verifyAndConsume(account, userCode, "reg");
	}

	public boolean verifyAndConsume(String account, String userCode, String purpose) {
		if (!StringUtils.hasText(account) || !StringUtils.hasText(userCode)) {
			return false;
		}
		String p = StringUtils.hasText(purpose) ? purpose.trim() : "reg";
		String k = normKey(account) + ":" + p;
		MemoryEntry e = codes.get(k);
		if (e == null || System.currentTimeMillis() > e.expireAtMillis) {
			codes.remove(k);
			return false;
		}
		if (!e.code.equals(userCode.trim())) {
			codes.remove(k);
			return false;
		}
		codes.remove(k);
		return true;
	}

	public boolean allowSend(String account, long intervalSeconds) {
		return allowSend(account, intervalSeconds, "reg");
	}

	public boolean allowSend(String account, long intervalSeconds, String purpose) {
		if (!StringUtils.hasText(account)) {
			return false;
		}
		String p = StringUtils.hasText(purpose) ? purpose.trim() : "reg";
		String k = normKey(account) + ":" + p + ":interval";
		long now = System.currentTimeMillis();
		Long last = lastSendAt.get(k);
		if (last != null && now - last < intervalSeconds * 1000L) {
			return false;
		}
		lastSendAt.put(k, now);
		return true;
	}

	private static String mask(String account) {
		String a = account.trim();
		if (a.length() <= 4) {
			return "***";
		}
		return a.substring(0, 3) + "****" + a.substring(a.length() - 2);
	}

	private record MemoryEntry(String code, long expireAtMillis) {
	}
}
