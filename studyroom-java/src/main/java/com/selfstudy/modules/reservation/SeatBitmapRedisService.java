package com.selfstudy.modules.reservation;

import com.selfstudy.config.ReservationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 座位按日时间槽占用：Redis Bitmap + Lua 原子检测并占用/释放。
 */
@Service
public class SeatBitmapRedisService {

	private static final Logger log = LoggerFactory.getLogger(SeatBitmapRedisService.class);

	private static final String LUA_TRY_OCCUPY =
			"local k=KEYS[1] local a=tonumber(ARGV[1]) local b=tonumber(ARGV[2]) " +
					"for i=a,b-1 do if redis.call('GETBIT',k,i)==1 then return -1 end end " +
					"for i=a,b-1 do redis.call('SETBIT',k,i,1) end return 1";

	private static final String LUA_RELEASE =
			"local k=KEYS[1] local a=tonumber(ARGV[1]) local b=tonumber(ARGV[2]) " +
					"for i=a,b-1 do redis.call('SETBIT',k,i,0) end return 1";

	private final StringRedisTemplate redis;
	private final ReservationProperties props;

	public SeatBitmapRedisService(StringRedisTemplate redis, ReservationProperties props) {
		this.redis = redis;
		this.props = props;
	}

	public String bitmapKey(Long seatId, String bizDate) {
		String d = bizDate.replace("-", "");
		return "study:bitmap:v1:" + seatId + ":" + d;
	}

	public int maxSlots() {
		LocalTimes t = parseDayBounds();
		int span = (t.endMin - t.startMin);
		return span / props.getSlotMinutes();
	}

	private LocalTimes parseDayBounds() {
		int ds = java.time.LocalTime.parse(props.getDayStart()).toSecondOfDay() / 60;
		int de = java.time.LocalTime.parse(props.getDayEnd()).toSecondOfDay() / 60;
		return new LocalTimes(ds, de);
	}

	private static final class LocalTimes {
		final int startMin;
		final int endMin;

		LocalTimes(int startMin, int endMin) {
			this.startMin = startMin;
			this.endMin = endMin;
		}
	}

	/**
	 * @return true 占用成功；false 槽位已被占用
	 */
	public boolean tryOccupyRange(Long seatId, String bizDate, int slotStartInclusive, int slotEndExclusive) {
		String key = bitmapKey(seatId, bizDate);
		DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_TRY_OCCUPY, Long.class);
		try {
			Long r = redis.execute(script, Collections.singletonList(key),
					String.valueOf(slotStartInclusive), String.valueOf(slotEndExclusive));
			return r != null && r == 1L;
		} catch (DataAccessException e) {
			log.error("bitmap tryOccupy failed seatId={} date={}", seatId, bizDate, e);
			throw e;
		}
	}

	public void releaseRange(Long seatId, String bizDate, int slotStartInclusive, int slotEndExclusive) {
		String key = bitmapKey(seatId, bizDate);
		DefaultRedisScript<Long> script = new DefaultRedisScript<>(LUA_RELEASE, Long.class);
		try {
			redis.execute(script, Collections.singletonList(key),
					String.valueOf(slotStartInclusive), String.valueOf(slotEndExclusive));
		} catch (DataAccessException e) {
			log.warn("bitmap release failed seatId={} date={}", seatId, bizDate, e);
		}
	}
}
