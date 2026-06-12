package com.selfstudy.modules.reservation;

import com.selfstudy.config.ReservationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 未签到超时释放：Redis ZSET，score 为截止时间戳（毫秒）。
 */
@Service
public class PendingCheckInRedisService {

	public static final String ZKEY = "study:pending:checkin:v1";

	private final StringRedisTemplate redis;
	private final ReservationProperties props;

	public PendingCheckInRedisService(StringRedisTemplate redis, ReservationProperties props) {
		this.redis = redis;
		this.props = props;
	}

	/**
	 * 未签到截止时间 = 预约时段开始时刻 + 宽限分钟（而非下单时刻）。
	 */
	public void scheduleAtSlotStart(Long appointmentId, String bizDate, int slotStartInclusive) {
		long graceMs = TimeUnit.MINUTES.toMillis(Math.max(1, props.getSignInGraceMinutes()));
		long deadline = computeCheckInDeadlineMs(bizDate, slotStartInclusive, graceMs);
		redis.opsForZSet().add(ZKEY, String.valueOf(appointmentId), deadline);
	}

	long computeCheckInDeadlineMs(String bizDate, int slotStartInclusive, long graceMs) {
		LocalTime dayStart = LocalTime.parse(props.getDayStart().trim());
		int slotMinutes = Math.max(1, props.getSlotMinutes());
		int startMin = dayStart.toSecondOfDay() / 60 + slotStartInclusive * slotMinutes;
		LocalDateTime slotStart = LocalDate.parse(bizDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
				.atTime(LocalTime.ofSecondOfDay((long) startMin * 60L));
		long deadline = slotStart.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() + graceMs;
		long now = System.currentTimeMillis();
		if (deadline < now) {
			deadline = now + graceMs;
		}
		return deadline;
	}

	public void cancelSchedule(Long appointmentId) {
		redis.opsForZSet().remove(ZKEY, String.valueOf(appointmentId));
	}

	/**
	 * 取出已到期的预约 id（最多 batch 条）。
	 */
	public Set<String> pollDueIds(long nowMillis, int batch) {
		return redis.opsForZSet().rangeByScore(ZKEY, 0, nowMillis, 0, batch);
	}

	public void ackProcessed(String appointmentId) {
		redis.opsForZSet().remove(ZKEY, appointmentId);
	}
}
