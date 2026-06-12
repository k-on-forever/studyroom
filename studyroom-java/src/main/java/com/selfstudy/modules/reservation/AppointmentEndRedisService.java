package com.selfstudy.modules.reservation;

import com.selfstudy.config.ReservationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 预约时段结束自动释放：Redis ZSET，score 为预约结束时刻（毫秒）。
 */
@Service
public class AppointmentEndRedisService {

	public static final String ZKEY = "study:session:end:v1";

	private final StringRedisTemplate redis;
	private final ReservationProperties props;

	public AppointmentEndRedisService(StringRedisTemplate redis, ReservationProperties props) {
		this.redis = redis;
		this.props = props;
	}

	/** 预约结束时刻 = bizDate + dayStart + slotEndExclusive × slotMinutes */
	public void scheduleAtSlotEnd(Long appointmentId, String bizDate, int slotEndExclusive) {
		long endMs = computeSlotEndMs(bizDate, slotEndExclusive);
		redis.opsForZSet().add(ZKEY, String.valueOf(appointmentId), endMs);
	}

	public long computeSlotEndMs(String bizDate, int slotEndExclusive) {
		LocalTime dayStart = LocalTime.parse(props.getDayStart().trim());
		int slotMinutes = Math.max(1, props.getSlotMinutes());
		int endMin = dayStart.toSecondOfDay() / 60 + slotEndExclusive * slotMinutes;
		LocalDateTime end = LocalDate.parse(bizDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE)
				.atTime(LocalTime.ofSecondOfDay((long) endMin * 60L));
		return end.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
	}

	public void cancelSchedule(Long appointmentId) {
		redis.opsForZSet().remove(ZKEY, String.valueOf(appointmentId));
	}

	public Set<String> pollDueIds(long nowMillis, int batch) {
		return redis.opsForZSet().rangeByScore(ZKEY, 0, nowMillis, 0, batch);
	}

	public void ackProcessed(String appointmentId) {
		redis.opsForZSet().remove(ZKEY, appointmentId);
	}
}
