package com.selfstudy.modules.bas.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selfstudy.common.exception.RRException;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.modules.bas.dao.BasFloorDao;
import com.selfstudy.modules.bas.dao.BasSeatAccessRuleDao;
import com.selfstudy.modules.bas.dao.BasSeatDao;
import com.selfstudy.modules.bas.dao.BasStudyRoomDao;
import com.selfstudy.modules.bas.entity.BasFloorEntity;
import com.selfstudy.modules.bas.entity.BasSeatAccessRuleEntity;
import com.selfstudy.modules.bas.entity.BasSeatEntity;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import com.selfstudy.modules.reservation.TimeSlotCodec;
import com.selfstudy.modules.user.entity.TbUserEntity;
import com.selfstudy.modules.user.service.TbUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台「时段锁座」：日期范围内每日时段全员不可订，或仅白名单用户可订；与询价/落库/选座 overlay 一致校验。
 */
@Service
public class BasSeatAccessRuleService extends ServiceImpl<BasSeatAccessRuleDao, BasSeatAccessRuleEntity> {

	public static final int LOCK_MODE_ALL = 0;
	public static final int LOCK_MODE_WHITELIST = 1;

	private final ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	private ReservationProperties reservationProperties;
	@Autowired
	private TimeSlotCodec timeSlotCodec;
	@Autowired
	private TbUserService tbUserService;
	@Autowired
	private BasSeatDao basSeatDao;
	@Autowired
	private BasStudyRoomDao basStudyRoomDao;
	@Autowired
	private BasFloorDao basFloorDao;

	public List<BasSeatAccessRuleEntity> listBySeatId(Long seatId) {
		if (seatId == null) {
			return List.of();
		}
		return list(new LambdaQueryWrapper<BasSeatAccessRuleEntity>()
				.eq(BasSeatAccessRuleEntity::getSeatId, seatId)
				.orderByDesc(BasSeatAccessRuleEntity::getId));
	}

	public void saveRule(BasSeatAccessRuleEntity in) {
		validateAndNormalize(in);
		Date now = new Date();
		if (in.getId() == null) {
			in.setCreateTime(now);
		}
		in.setUpdateTime(now);
		saveOrUpdate(in);
	}

	public void validateAndNormalize(BasSeatAccessRuleEntity in) {
		if (in.getSeatId() == null) {
			throw new RRException("座位不能为空");
		}
		if (!StringUtils.hasText(in.getDateFrom()) || !StringUtils.hasText(in.getDateTo())) {
			throw new RRException("日期范围不能为空");
		}
		LocalDate df;
		LocalDate dt;
		try {
			df = LocalDate.parse(in.getDateFrom().trim());
			dt = LocalDate.parse(in.getDateTo().trim());
		} catch (DateTimeParseException e) {
			throw new RRException("日期格式须为 yyyy-MM-dd");
		}
		if (df.isAfter(dt)) {
			throw new RRException("结束日不能早于起始日");
		}
		LocalTime tf = parseHm(in.getTimeFrom());
		LocalTime tt = parseHm(in.getTimeTo());
		if (!tf.isBefore(tt)) {
			throw new RRException("每日结束时刻须晚于开始时刻");
		}
		in.setDateFrom(df.toString());
		in.setDateTo(dt.toString());
		in.setTimeFrom(tf.toString());
		in.setTimeTo(tt.toString());
		int mode = in.getLockMode() == null ? LOCK_MODE_ALL : in.getLockMode();
		if (mode != LOCK_MODE_ALL && mode != LOCK_MODE_WHITELIST) {
			throw new RRException("锁座模式不合法");
		}
		in.setLockMode(mode);
		if (mode == LOCK_MODE_WHITELIST) {
			List<String> phones = parseWhitelistMobileInput(in.getWhitelistUserIds());
			if (phones.isEmpty()) {
				throw new RRException("「仅白名单可订」须填写至少一个手机号");
			}
			for (String p : phones) {
				if (p.length() < 10 || p.length() > 15) {
					throw new RRException("手机号格式异常（仅数字 10～15 位）：" + p);
				}
			}
			try {
				in.setWhitelistUserIds(objectMapper.writeValueAsString(phones));
			} catch (Exception e) {
				throw new RRException("白名单序列化失败");
			}
		} else {
			in.setWhitelistUserIds(null);
		}
		if (in.getEnabled() == null) {
			in.setEnabled(1);
		}
	}

	private static LocalTime parseHm(String s) {
		if (!StringUtils.hasText(s)) {
			throw new RRException("时刻不能为空");
		}
		String t = s.trim();
		try {
			if (t.length() <= 5) {
				return LocalTime.parse(t + ":00");
			}
			return LocalTime.parse(t);
		} catch (DateTimeParseException e) {
			throw new RRException("时刻格式须为 HH:mm");
		}
	}

	/** 管理端输入：逗号分隔或 JSON 字符串数组，归一为纯数字手机号列表 */
	public List<String> parseWhitelistMobileInput(String raw) {
		if (!StringUtils.hasText(raw)) {
			return List.of();
		}
		String t = raw.trim();
		try {
			if (t.startsWith("[")) {
				JsonNode arr = objectMapper.readTree(t);
				if (!arr.isArray()) {
					return List.of();
				}
				Set<String> out = new LinkedHashSet<>();
				for (JsonNode n : arr) {
					if (n == null || n.isNull()) {
						continue;
					}
					if (n.isTextual()) {
						String d = normalizeMobileDigits(n.asText());
						if (StringUtils.hasText(d)) {
							out.add(d);
						}
					} else if (n.isNumber()) {
						// 旧数据 [1,2] 用户ID：保存时不再走此分支；若误填纯数字 JSON 当手机号用 long 转 string
						String d = normalizeMobileDigits(n.toString());
						if (StringUtils.hasText(d)) {
							out.add(d);
						}
					}
				}
				return new ArrayList<>(out);
			}
		} catch (Exception e) {
			throw new RRException("白名单格式错误：请使用逗号分隔手机号，或 JSON 如 [\"13800138000\",\"13900139000\"]");
		}
		Set<String> out = new LinkedHashSet<>();
		for (String p : t.split("[,，\\s]+")) {
			if (p.isEmpty()) {
				continue;
			}
			String d = normalizeMobileDigits(p);
			if (StringUtils.hasText(d)) {
				out.add(d);
			}
		}
		return new ArrayList<>(out);
	}

	static String normalizeMobileDigits(String s) {
		if (s == null) {
			return "";
		}
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= '0' && c <= '9') {
				b.append(c);
			}
		}
		return b.toString();
	}

	/**
	 * 白名单是否允许该用户：新数据为手机号数组；兼容旧库 JSON 用户 ID 数组。
	 */
	boolean whitelistAllowsUser(Long userId, String whitelistStored) {
		if (userId == null || !StringUtils.hasText(whitelistStored)) {
			return false;
		}
		String raw = whitelistStored.trim();
		try {
			if (raw.startsWith("[")) {
				JsonNode arr = objectMapper.readTree(raw);
				if (!arr.isArray()) {
					return false;
				}
				List<Long> legacyIds = new ArrayList<>();
				List<String> phones = new ArrayList<>();
				for (JsonNode n : arr) {
					if (n == null || n.isNull()) {
						continue;
					}
					if (n.isIntegralNumber()) {
						legacyIds.add(n.longValue());
					} else if (n.isTextual()) {
						String d = normalizeMobileDigits(n.asText());
						if (StringUtils.hasText(d)) {
							phones.add(d);
						}
					}
				}
				boolean byId = !legacyIds.isEmpty() && legacyIds.contains(userId);
				boolean byPhone = !phones.isEmpty() && userMobileInList(userId, phones);
				// 仅数字 ID 的旧数据：phones 为空
				if (!legacyIds.isEmpty() && phones.isEmpty()) {
					return byId;
				}
				// 仅手机号
				if (legacyIds.isEmpty() && !phones.isEmpty()) {
					return byPhone;
				}
				// 混合或空
				return byId || byPhone;
			}
		} catch (Exception e) {
			return false;
		}
		List<String> phones = parseWhitelistMobileInput(raw);
		return userMobileInList(userId, phones);
	}

	private boolean userMobileInList(Long userId, List<String> normalizedPhones) {
		if (normalizedPhones == null || normalizedPhones.isEmpty()) {
			return false;
		}
		TbUserEntity u = tbUserService.getById(userId);
		String m = u == null ? null : normalizeMobileDigits(u.getMobile());
		return StringUtils.hasText(m) && normalizedPhones.contains(m);
	}

	/**
	 * 当前用户可享的「仅白名单可订」策略（小程序「入座」展示用）。
	 * 凭证为注册手机号，无单独密钥。
	 */
	public Map<String, Object> whitelistPrivilegesForUser(Long userId) {
		Map<String, Object> out = new HashMap<>();
		if (userId == null) {
			out.put("mobileMask", "");
			out.put("credentialHint", "请先登录");
			out.put("rules", List.of());
			return out;
		}
		TbUserEntity u = tbUserService.getById(userId);
		String mobile = u == null ? null : normalizeMobileDigits(u.getMobile());
		out.put("mobileMask", maskMobile(mobile));
		out.put("credentialHint",
				"门店授权：与登录手机号一致方可订（非选座占位）。请在「我的」完善手机号，并与后台「时段策略」中填写的号码一致。");
		if (!StringUtils.hasText(mobile)) {
			out.put("rules", List.of());
			return out;
		}
		LocalDate today = LocalDate.now();
		List<BasSeatAccessRuleEntity> candidates = list(new LambdaQueryWrapper<BasSeatAccessRuleEntity>()
				.eq(BasSeatAccessRuleEntity::getEnabled, 1)
				.eq(BasSeatAccessRuleEntity::getLockMode, LOCK_MODE_WHITELIST)
				.ge(BasSeatAccessRuleEntity::getDateTo, today.toString())
				.orderByDesc(BasSeatAccessRuleEntity::getId));
		List<Map<String, Object>> rules = new ArrayList<>();
		for (BasSeatAccessRuleEntity rule : candidates) {
			if (!whitelistAllowsUser(userId, rule.getWhitelistUserIds())) {
				continue;
			}
			Map<String, Object> row = new HashMap<>();
			row.put("ruleId", rule.getId());
			row.put("seatId", rule.getSeatId());
			row.put("dateFrom", rule.getDateFrom());
			row.put("dateTo", rule.getDateTo());
			row.put("timeFrom", shortHm(rule.getTimeFrom()));
			row.put("timeTo", shortHm(rule.getTimeTo()));
			row.put("remark", rule.getRemark());
			enrichSeatRow(row, rule.getSeatId());
			rules.add(row);
		}
		out.put("rules", rules);
		return out;
	}

	private static String shortHm(String t) {
		if (!StringUtils.hasText(t)) {
			return "";
		}
		String s = t.trim();
		return s.length() >= 5 ? s.substring(0, 5) : s;
	}

	private static String maskMobile(String mobile) {
		if (!StringUtils.hasText(mobile)) {
			return "未绑定";
		}
		if (mobile.length() < 7) {
			return mobile;
		}
		return mobile.substring(0, 3) + "****" + mobile.substring(mobile.length() - 4);
	}

	private void enrichSeatRow(Map<String, Object> row, Long seatId) {
		if (seatId == null) {
			return;
		}
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
					if (floor != null) {
						row.put("floor", floor.getFloorName());
					}
				}
			}
		}
	}

	/**
	 * 是否「门店授权」预约：落在仅白名单可订时段且当前用户在名单内。
	 * 此类预约不因未签到宽限期提前释放，仅在预约时段结束时刻释放。
	 */
	public boolean isStoreAuthBooking(Long userId, Long seatId, TimeSlotCodec.ParsedSeatDay parsed) {
		if (userId == null || seatId == null || parsed == null) {
			return false;
		}
		List<BasSeatAccessRuleEntity> rules = listActiveForSeatOnDate(seatId, parsed.bizDate);
		for (BasSeatAccessRuleEntity rule : rules) {
			if (rule.getLockMode() != null && rule.getLockMode() == LOCK_MODE_WHITELIST
					&& timeWindowOverlaps(parsed, rule)
					&& whitelistAllowsUser(userId, rule.getWhitelistUserIds())) {
				return true;
			}
		}
		return false;
	}

	/** 不可订时返回原因文案；可订返回 null。 */
	public String bookDenyReason(Long userId, Long seatId, TimeSlotCodec.ParsedSeatDay parsed) {
		if (userId == null || seatId == null || parsed == null) {
			return null;
		}
		List<BasSeatAccessRuleEntity> rules = listActiveForSeatOnDate(seatId, parsed.bizDate);
		return firstDenyReason(userId, parsed, rules);
	}

	/**
	 * 若当前用户不可订该座位该时段，抛出 {@link RRException}（询价等接口用）。
	 */
	public void assertUserMayBook(Long userId, Long seatId, TimeSlotCodec.ParsedSeatDay parsed) {
		String deny = bookDenyReason(userId, seatId, parsed);
		if (deny != null) {
			throw new RRException(deny);
		}
	}

	public List<BasSeatAccessRuleEntity> listActiveForSeatOnDate(Long seatId, String bizDate) {
		if (seatId == null || !StringUtils.hasText(bizDate)) {
			return List.of();
		}
		String d = bizDate.trim();
		return list(new LambdaQueryWrapper<BasSeatAccessRuleEntity>()
				.eq(BasSeatAccessRuleEntity::getSeatId, seatId)
				.eq(BasSeatAccessRuleEntity::getEnabled, 1)
				.le(BasSeatAccessRuleEntity::getDateFrom, d)
				.ge(BasSeatAccessRuleEntity::getDateTo, d));
	}

	public List<BasSeatAccessRuleEntity> listActiveForSeatsOnDate(List<Long> seatIds, String bizDate) {
		if (seatIds == null || seatIds.isEmpty() || !StringUtils.hasText(bizDate)) {
			return List.of();
		}
		String d = bizDate.trim();
		return list(new LambdaQueryWrapper<BasSeatAccessRuleEntity>()
				.in(BasSeatAccessRuleEntity::getSeatId, seatIds)
				.eq(BasSeatAccessRuleEntity::getEnabled, 1)
				.le(BasSeatAccessRuleEntity::getDateFrom, d)
				.ge(BasSeatAccessRuleEntity::getDateTo, d));
	}

	/**
	 * 合并选座 overlay：与当前所选时段重叠且对当前用户不可订时，视为他人占用（灰显）。
	 */
	public void mergeIntoOverlay(Long userId, String bizDate, String seatDayOptional, List<BasSeatEntity> seats,
			Map<Long, Map<String, Object>> acc) {
		if (seatDayOptional == null || seatDayOptional.isBlank() || seats == null || seats.isEmpty()) {
			return;
		}
		TimeSlotCodec.ParsedSeatDay parsed;
		try {
			parsed = timeSlotCodec.parseForSeatBrowseOverlay(seatDayOptional.trim(), null);
		} catch (Exception e) {
			return;
		}
		if (!bizDate.equals(parsed.bizDate)) {
			return;
		}
		if (parsed.slotEndExclusive <= parsed.slotStartInclusive) {
			return;
		}
		List<Long> seatIds = seats.stream().map(BasSeatEntity::getId).filter(id -> id != null).collect(Collectors.toList());
		if (seatIds.isEmpty()) {
			return;
		}
		List<BasSeatAccessRuleEntity> all = listActiveForSeatsOnDate(seatIds, bizDate);
		if (all.isEmpty()) {
			return;
		}
		Map<Long, List<BasSeatAccessRuleEntity>> bySeat = new HashMap<>();
		for (BasSeatAccessRuleEntity r : all) {
			bySeat.computeIfAbsent(r.getSeatId(), k -> new ArrayList<>()).add(r);
		}
		for (BasSeatEntity s : seats) {
			Long sid = s.getId();
			if (sid == null) {
				continue;
			}
			List<BasSeatAccessRuleEntity> rules = bySeat.get(sid);
			if (rules == null || rules.isEmpty()) {
				continue;
			}
			for (BasSeatAccessRuleEntity rule : rules) {
				if (!timeWindowOverlaps(parsed, rule)) {
					continue;
				}
				int mode = rule.getLockMode() == null ? LOCK_MODE_ALL : rule.getLockMode();
				boolean deny = mode == LOCK_MODE_ALL
						|| !whitelistAllowsUser(userId, rule.getWhitelistUserIds());
				if (!deny) {
					continue;
				}
				Map<String, Object> row = acc.computeIfAbsent(sid, k -> {
					Map<String, Object> m = new HashMap<>();
					m.put("seatId", sid);
					m.put("mine", false);
					m.put("lockedByOther", false);
					m.put("appointmentBooked", false);
					m.put("appointmentMine", false);
					return m;
				});
				row.put("lockedByOther", true);
				appendAccessBlockSeatDay(row, bizDate, rule);
			}
		}
	}

	private String firstDenyReason(Long userId, TimeSlotCodec.ParsedSeatDay parsed, List<BasSeatAccessRuleEntity> rules) {
		for (BasSeatAccessRuleEntity rule : rules) {
			if (!timeWindowOverlaps(parsed, rule)) {
				continue;
			}
			int mode = rule.getLockMode() == null ? LOCK_MODE_ALL : rule.getLockMode();
			String from = rule.getDateFrom() != null ? rule.getDateFrom() : parsed.bizDate;
			String to = rule.getDateTo() != null ? rule.getDateTo() : parsed.bizDate;
			String dateRange = from.equals(to) ? from : (from + " ~ " + to);
			String win = dateRange + " " + shortHm(rule.getTimeFrom()) + " 至 " + shortHm(rule.getTimeTo());
			if (mode == LOCK_MODE_ALL) {
				return "该座位在 " + win + " 已被后台锁定，无法预约";
			}
			if (!whitelistAllowsUser(userId, rule.getWhitelistUserIds())) {
				return "该座位在 " + win + " 仅限指定手机号用户预约";
			}
		}
		return null;
	}

	private static void appendAccessBlockSeatDay(Map<String, Object> row, String bizDate,
			BasSeatAccessRuleEntity rule) {
		if (rule.getTimeFrom() == null || rule.getTimeTo() == null) {
			return;
		}
		String from = rule.getDateFrom() != null ? rule.getDateFrom() : bizDate;
		String to = rule.getDateTo() != null ? rule.getDateTo() : bizDate;
		String t;
		if (from.equals(to)) {
			t = from + "/" + shortHm(rule.getTimeFrom()) + "-" + shortHm(rule.getTimeTo());
		} else {
			t = from + " ~ " + to + "/" + shortHm(rule.getTimeFrom()) + "-" + shortHm(rule.getTimeTo());
		}
		Object cur = row.get("accessBlockSeatDay");
		if (cur == null || !StringUtils.hasText(String.valueOf(cur))) {
			row.put("accessBlockSeatDay", t);
			return;
		}
		String s = String.valueOf(cur);
		if (!s.contains(t)) {
			row.put("accessBlockSeatDay", s + "；" + t);
		}
	}

	private boolean timeWindowOverlaps(TimeSlotCodec.ParsedSeatDay parsed, BasSeatAccessRuleEntity rule) {
		LocalTime dayStart = LocalTime.parse(reservationProperties.getDayStart().trim());
		int ds = dayStart.toSecondOfDay() / 60;
		int step = Math.max(1, reservationProperties.getSlotMinutes());
		int selLo = ds + parsed.slotStartInclusive * step;
		int selHi = ds + parsed.slotEndExclusive * step;
		LocalTime tf = LocalTime.parse(rule.getTimeFrom().trim());
		LocalTime tt = LocalTime.parse(rule.getTimeTo().trim());
		int rf = tf.toSecondOfDay() / 60;
		int rt = tt.toSecondOfDay() / 60;
		return selLo < rt && selHi > rf;
	}
}
