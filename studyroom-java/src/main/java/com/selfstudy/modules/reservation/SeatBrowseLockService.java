package com.selfstudy.modules.reservation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.common.exception.RRException;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.modules.bas.dao.BasAppointmentDao;
import com.selfstudy.modules.bas.dao.BasFloorDao;
import com.selfstudy.modules.bas.dao.BasSeatDao;
import com.selfstudy.modules.bas.dao.BasStudyRoomDao;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.entity.BasFloorEntity;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import com.selfstudy.modules.bas.service.BasSeatAccessRuleService;
import com.selfstudy.modules.bas.service.BasSeatService;
import com.selfstudy.modules.reservation.TimeSlotCodec.ParsedSeatDay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 选座页「暂锁」：Redis 独占键 + TTL；与正式预约、访问规则合并为 overlay 灰显。
 */
@Service
public class SeatBrowseLockService {

	private static final String KEY_PREFIX = "study:browse:slot:";

	private final Logger log = LoggerFactory.getLogger(getClass());
	private final StringRedisTemplate redis;
	private final TimeSlotCodec timeSlotCodec;
	private final BasSeatService basSeatService;
	private final BasAppointmentDao basAppointmentDao;
	private final BasSeatDao basSeatDao;
	private final BasStudyRoomDao basStudyRoomDao;
	private final BasFloorDao basFloorDao;
	private final ReservationProperties reservationProperties;
	private final BasSeatAccessRuleService basSeatAccessRuleService;

	@Value("${study.browse-lock.enabled:false}")
	private boolean browseLockEnabled;

	@Value("${study.browse-lock.ttl-seconds:900}")
	private int ttlSeconds;

	public SeatBrowseLockService(StringRedisTemplate redis, TimeSlotCodec timeSlotCodec, BasSeatService basSeatService,
			BasAppointmentDao basAppointmentDao, BasSeatDao basSeatDao, BasStudyRoomDao basStudyRoomDao,
			BasFloorDao basFloorDao, ReservationProperties reservationProperties,
			BasSeatAccessRuleService basSeatAccessRuleService) {
		this.redis = redis;
		this.timeSlotCodec = timeSlotCodec;
		this.basSeatService = basSeatService;
		this.basAppointmentDao = basAppointmentDao;
		this.basSeatDao = basSeatDao;
		this.basStudyRoomDao = basStudyRoomDao;
		this.basFloorDao = basFloorDao;
		this.reservationProperties = reservationProperties;
		this.basSeatAccessRuleService = basSeatAccessRuleService;
	}

	public String slotKey(Long seatId, ParsedSeatDay p) {
		return KEY_PREFIX + seatId + ":" + p.bizDate + ":" + p.slotStartInclusive + ":" + p.slotEndExclusive;
	}

	public boolean isBlockedByOther(Long userId, Long seatId, ParsedSeatDay p) {
		if (!browseLockEnabled) {
			return false;
		}
		String key = slotKey(seatId, p);
		try {
			String v = redis.opsForValue().get(key);
			if (v == null) {
				return false;
			}
			return !String.valueOf(userId).equals(v);
		} catch (DataAccessException e) {
			log.warn("browse lock read failed: {}", e.getMessage());
			return false;
		}
	}

	public void clearIfMine(Long userId, Long seatId, ParsedSeatDay p) {
		String key = slotKey(seatId, p);
		try {
			String v = redis.opsForValue().get(key);
			if (String.valueOf(userId).equals(v)) {
				redis.delete(key);
			}
		} catch (DataAccessException e) {
			log.warn("browse lock clear failed: {}", e.getMessage());
		}
	}

	public boolean tryHold(Long userId, Long seatId, String seatDay, BasStudyRoomEntity room) {
		if (!browseLockEnabled) {
			return true;
		}
		Objects.requireNonNull(userId, "userId");
		Objects.requireNonNull(seatId, "seatId");
		ParsedSeatDay p = timeSlotCodec.parse(seatDay, room);
		String key = slotKey(seatId, p);
		String uid = String.valueOf(userId);
		try {
			Boolean ok = redis.opsForValue().setIfAbsent(key, uid, Duration.ofSeconds(Math.max(60, ttlSeconds)));
			if (Boolean.TRUE.equals(ok)) {
				return true;
			}
			String cur = redis.opsForValue().get(key);
			if (uid.equals(cur)) {
				redis.expire(key, Duration.ofSeconds(Math.max(60, ttlSeconds)));
				return true;
			}
			return false;
		} catch (DataAccessException e) {
			log.error("browse lock hold failed key={}", key, e);
			throw new RRException("暂锁失败：Redis 不可用");
		}
	}

	public void release(Long userId, Long seatId, String seatDay, BasStudyRoomEntity room) {
		ParsedSeatDay p = timeSlotCodec.parse(seatDay, room);
		String key = slotKey(seatId, p);
		try {
			String cur = redis.opsForValue().get(key);
			if (String.valueOf(userId).equals(cur)) {
				redis.delete(key);
			}
		} catch (DataAccessException e) {
			log.warn("browse lock release failed key={}", key, e);
		}
	}

	public List<Map<String, Object>> listMine(Long userId) {
		String uid = String.valueOf(userId);
		List<Map<String, Object>> out = new ArrayList<>();
		ScanOptions opt = ScanOptions.scanOptions().match(KEY_PREFIX + "*").count(200).build();
		try (Cursor<String> cursor = redis.scan(opt)) {
			while (cursor.hasNext()) {
				String key = cursor.next();
				String v;
				try {
					v = redis.opsForValue().get(key);
				} catch (DataAccessException e) {
					continue;
				}
				if (!uid.equals(v)) {
					continue;
				}
				ParsedKey pk = parseKey(key);
				if (pk == null) {
					continue;
				}
				long ttl = 0L;
				try {
					Long t = redis.getExpire(key);
					ttl = t != null && t > 0 ? t : 0L;
				} catch (DataAccessException ignored) {
				}
				Map<String, Object> row = new HashMap<>();
				row.put("seatId", pk.seatId);
				row.put("bizDate", pk.bizDate);
				row.put("slotStart", pk.slotStart);
				row.put("slotEnd", pk.slotEnd);
				row.put("seatDay", pk.bizDate + "/" + slotHm(pk.slotStart) + "-" + slotHm(pk.slotEnd));
				row.put("ttlSeconds", ttl);
				enrichSeatLocation(row, pk.seatId);
				out.add(row);
			}
		} catch (Exception e) {
			log.warn("browse lock scan mine: {}", e.getMessage());
		}
		return out;
	}

	private void enrichSeatLocation(Map<String, Object> row, long seatId) {
		BasSeatEntity seat = basSeatDao.selectById(seatId);
		if (seat == null) {
			return;
		}
		row.put("seatName", seat.getSeatName());
		if (seat.getRoomId() != null) {
			BasStudyRoomEntity room = basStudyRoomDao.selectById(seat.getRoomId());
			if (room != null) {
				row.put("roomName", room.getRoomName());
				if (room.getFloorId() != null) {
					BasFloorEntity floor = basFloorDao.selectById(room.getFloorId());
					if (floor != null && StringUtils.hasText(floor.getFloorName())) {
						row.put("floor", floor.getFloorName());
					}
				}
			}
		}
	}

	public List<Map<String, Object>> overlay(Long userId, Long roomId, String bizDate, String seatDayOptional,
			BasStudyRoomEntity room) {
		if (roomId == null || bizDate == null || bizDate.isBlank()) {
			return List.of();
		}
		String d = bizDate.trim();
		ParsedSeatDay selParsed = null;
		if (seatDayOptional != null && !seatDayOptional.isBlank()) {
			try {
				selParsed = timeSlotCodec.parseForSeatBrowseOverlay(seatDayOptional.trim(), room);
				if (!d.equals(selParsed.bizDate)) {
					selParsed = null;
				}
			} catch (Exception e) {
				log.debug("overlay browse sel parse: {}", e.getMessage());
			}
		}
		List<BasSeatEntity> seats = basSeatService.getSeatByRoom(roomId);
		Map<Long, Map<String, Object>> acc = new LinkedHashMap<>();
		mergeAppointmentOverlap(userId, d, seatDayOptional, seats, acc);
		basSeatAccessRuleService.mergeIntoOverlay(userId, d, seatDayOptional, seats, acc);
		List<Map<String, Object>> out = new ArrayList<>();
		for (Map<String, Object> row : acc.values()) {
			attachOccupancyDetail(row);
			boolean show = Boolean.TRUE.equals(row.get("lockedByOther"))
					|| Boolean.TRUE.equals(row.get("appointmentBooked"));
			if (show) {
				out.add(row);
			}
		}
		return out;
	}

	private static boolean browseSlotOverlaps(ParsedSeatDay sel, ParsedKey pk) {
		return sel.slotStartInclusive < pk.slotEnd && sel.slotEndExclusive > pk.slotStart;
	}

	/** 合并占用时段文案，供小程序弹窗展示 */
	private static void attachOccupancyDetail(Map<String, Object> row) {
		LinkedHashSet<String> parts = new LinkedHashSet<>();
		appendDetailPart(parts, row.get("busySeatDay"));
		appendDetailPart(parts, row.get("accessBlockSeatDay"));
		if (!parts.isEmpty()) {
			row.put("occupancyDetail", String.join("；", parts));
		}
	}

	private static void appendDetailPart(LinkedHashSet<String> parts, Object raw) {
		if (raw == null) {
			return;
		}
		String s = String.valueOf(raw).trim();
		if (!s.isEmpty()) {
			parts.add(s);
		}
	}

	private static Map<String, Object> baseOverlayRow(Long seatId) {
		Map<String, Object> row = new HashMap<>();
		row.put("seatId", seatId);
		row.put("mine", false);
		row.put("lockedByOther", false);
		row.put("appointmentBooked", false);
		row.put("appointmentMine", false);
		return row;
	}

	private void mergeAppointmentOverlap(Long userId, String bizDate, String seatDayOptional,
			List<BasSeatEntity> seats, Map<Long, Map<String, Object>> acc) {
		if (seatDayOptional == null || seatDayOptional.isBlank() || seats == null || seats.isEmpty()) {
			return;
		}
		TimeSlotCodec.ParsedSeatDay parsed;
		try {
			parsed = timeSlotCodec.parseForSeatBrowseOverlay(seatDayOptional.trim(), null);
		} catch (Exception e) {
			log.warn("overlay appointment merge parse failed seatDay={}: {}", seatDayOptional, e.getMessage());
			return;
		}
		if (!bizDate.equals(parsed.bizDate)) {
			return;
		}
		if (parsed.slotEndExclusive <= parsed.slotStartInclusive) {
			return;
		}
		List<Long> seatIds = new ArrayList<>();
		for (BasSeatEntity s : seats) {
			if (s.getId() != null) {
				seatIds.add(s.getId());
			}
		}
		if (seatIds.isEmpty()) {
			return;
		}
		LocalTime dayStartLt = LocalTime.parse(reservationProperties.getDayStart());
		LocalTime dayEndLt = LocalTime.parse(reservationProperties.getDayEnd());
		int ds = dayStartLt.toSecondOfDay() / 60;
		int de = dayEndLt.toSecondOfDay() / 60;
		int globalStep = Math.max(1, reservationProperties.getSlotMinutes());

		LambdaQueryWrapper<BasAppointmentEntity> w = new LambdaQueryWrapper<>();
		w.in(BasAppointmentEntity::getSeatId, seatIds).eq(BasAppointmentEntity::getBizDate, bizDate)
				.in(BasAppointmentEntity::getSeatState, 0, 1);
		List<BasAppointmentEntity> apps = basAppointmentDao.selectList(w);
		for (BasAppointmentEntity ap : apps) {
			if (!appointmentOverlapsSelectedWindow(ap, parsed, ds, de, globalStep)) {
				continue;
			}
			Long sid = ap.getSeatId();
			if (sid == null) {
				continue;
			}
			Map<String, Object> row = acc.computeIfAbsent(sid, SeatBrowseLockService::baseOverlayRow);
			row.put("appointmentBooked", true);
			if (userId != null && userId.equals(ap.getUserId())) {
				row.put("appointmentMine", true);
			}
			appendBusySeatDay(row, ap.getSeatDay());
		}
	}

	private static void appendBusySeatDay(Map<String, Object> row, String seatDay) {
		if (!StringUtils.hasText(seatDay)) {
			return;
		}
		String t = seatDay.trim();
		Object cur = row.get("busySeatDay");
		if (cur == null || !StringUtils.hasText(String.valueOf(cur))) {
			row.put("busySeatDay", t);
			return;
		}
		String s = String.valueOf(cur);
		if (s.contains(t)) {
			return;
		}
		row.put("busySeatDay", s + "；" + t);
	}

	private boolean appointmentOverlapsSelectedWindow(BasAppointmentEntity ap, ParsedSeatDay parsed,
			int dayStartMin, int dayEndMin, int globalStep) {
		int selLo = dayStartMin + parsed.slotStartInclusive * globalStep;
		int selHi = dayStartMin + parsed.slotEndExclusive * globalStep;
		if (selHi <= selLo) {
			return false;
		}
		String rawSeatDay = ap.getSeatDay();
		if (rawSeatDay != null && !rawSeatDay.isBlank() && rawSeatDay.indexOf('/') > 0) {
			try {
				TimeSlotCodec.ParsedSeatDay apWin = timeSlotCodec.parseForSeatBrowseOverlay(rawSeatDay.trim(), null);
				if (parsed.bizDate.equals(apWin.bizDate)) {
					int apLo = dayStartMin + apWin.slotStartInclusive * globalStep;
					int apHi = dayStartMin + apWin.slotEndExclusive * globalStep;
					if (apHi > apLo && apLo < selHi && apHi > selLo) {
						return true;
					}
				}
			} catch (Exception e) {
				log.debug("overlay ap seatDay parse: {}", e.getMessage());
			}
		}
		if (ap.getSlotStart() == null || ap.getSlotEnd() == null) {
			return false;
		}
		int ss = ap.getSlotStart();
		int se = ap.getSlotEnd();
		if (se <= ss) {
			return false;
		}
		LinkedHashSet<Integer> steps = new LinkedHashSet<>();
		steps.add(globalStep);
		if (globalStep == 60) {
			steps.add(30);
		}
		for (int step : steps) {
			int apLo = dayStartMin + ss * step;
			int apHi = dayStartMin + se * step;
			if (apLo < dayStartMin || apHi > dayEndMin) {
				continue;
			}
			if (apLo < selHi && apHi > selLo) {
				return true;
			}
		}
		return false;
	}

	private String slotHm(int slotIdx) {
		LocalTime dayStart = LocalTime.parse(reservationProperties.getDayStart());
		int baseMin = dayStart.toSecondOfDay() / 60;
		int step = Math.max(1, reservationProperties.getSlotMinutes());
		int m = baseMin + slotIdx * step;
		int h = m / 60;
		int mm = m % 60;
		return (h < 10 ? "0" : "") + h + ":" + (mm < 10 ? "0" : "") + mm;
	}

	private record ParsedKey(long seatId, String bizDate, int slotStart, int slotEnd) {
	}

	private static ParsedKey parseKey(String key) {
		if (key == null || !key.startsWith(KEY_PREFIX)) {
			return null;
		}
		String rest = key.substring(KEY_PREFIX.length());
		int i = rest.indexOf(':');
		if (i < 0) {
			return null;
		}
		try {
			long seatId = Long.parseLong(rest.substring(0, i));
			String tail = rest.substring(i + 1);
			int j = tail.lastIndexOf(':');
			if (j < 0) {
				return null;
			}
			int k = tail.lastIndexOf(':', j - 1);
			if (k < 0) {
				return null;
			}
			String biz = tail.substring(0, k);
			int s0 = Integer.parseInt(tail.substring(k + 1, j));
			int s1 = Integer.parseInt(tail.substring(j + 1));
			return new ParsedKey(seatId, biz, s0, s1);
		} catch (Exception e) {
			return null;
		}
	}
}
