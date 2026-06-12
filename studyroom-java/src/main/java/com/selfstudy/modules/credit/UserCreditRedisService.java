package com.selfstudy.modules.credit;

import com.selfstudy.common.exception.RRException;
import com.selfstudy.config.ReservationProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 用户信用分：Redis 存储整数分，低于阈值禁止预约。
 */
@Service
public class UserCreditRedisService {

	private static final String KEY_PREFIX = "study:credit:v1:";

	private final StringRedisTemplate redis;
	private final ReservationProperties props;

	public UserCreditRedisService(StringRedisTemplate redis, ReservationProperties props) {
		this.redis = redis;
		this.props = props;
	}

	private String key(Long userId) {
		return KEY_PREFIX + userId;
	}

	public void ensureInitialized(Long userId) {
		String k = key(userId);
		try {
			Boolean set = redis.opsForValue().setIfAbsent(k, String.valueOf(props.getInitialCreditScore()));
			if (Boolean.TRUE.equals(set)) {
				return;
			}
			String v = redis.opsForValue().get(k);
			if (v == null) {
				redis.opsForValue().set(k, String.valueOf(props.getInitialCreditScore()));
			}
		} catch (DataAccessException e) {
			throw e;
		}
	}

	public int getScore(Long userId) {
		ensureInitialized(userId);
		String v = redis.opsForValue().get(key(userId));
		return v == null ? props.getInitialCreditScore() : Integer.parseInt(v);
	}

	public void assertCanBook(Long userId) {
		if (!props.isCreditEnabled()) {
			return;
		}
		int s = getScore(userId);
		if (s < props.getMinCreditScore()) {
			throw new RRException("信用分过低（当前 " + s + "），暂时无法预约");
		}
	}

	public int addPenalty(Long userId, int deltaNeg) {
		if (!props.isCreditEnabled()) {
			return props.getInitialCreditScore();
		}
		String k = key(userId);
		Long v = redis.opsForValue().increment(k, -Math.abs(deltaNeg));
		return v == null ? props.getMinCreditScore() - 1 : v.intValue();
	}
}
