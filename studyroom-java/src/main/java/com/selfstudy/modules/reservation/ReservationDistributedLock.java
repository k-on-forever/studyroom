package com.selfstudy.modules.reservation;

import com.selfstudy.config.ReservationProperties;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 同一座位 + 业务日互斥，防止并发预约写 Bitmap 竞态（Redisson {@link RLock}）。
 */
@Component
public class ReservationDistributedLock {

	private final RedissonClient redisson;
	private final ReservationProperties props;

	public ReservationDistributedLock(RedissonClient redisson, ReservationProperties props) {
		this.redisson = redisson;
		this.props = props;
	}

	public String lockKey(Long seatId, String bizDate) {
		return "study:lock:seat:" + seatId + ":" + bizDate.replace("-", "");
	}

	public <T> T execute(Long seatId, String bizDate, Callable<T> action) throws Exception {
		String key = lockKey(seatId, bizDate);
		RLock lock = redisson.getLock(key);
		long waitSec = Math.max(1, props.getLockWaitSeconds());
		long leaseSec = Math.max(1, props.getLockLeaseSeconds());
		boolean acquired = lock.tryLock(waitSec, leaseSec, TimeUnit.SECONDS);
		if (!acquired) {
			throw new IllegalStateException("获取座位锁超时，请稍后重试");
		}
		try {
			return action.call();
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
}
