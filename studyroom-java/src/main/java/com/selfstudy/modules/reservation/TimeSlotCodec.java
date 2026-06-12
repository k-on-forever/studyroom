package com.selfstudy.modules.reservation;

import com.selfstudy.common.exception.RRException;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.modules.bas.entity.BasStudyRoomEntity;
import com.selfstudy.modules.sys.service.ReservationRuleConfigService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 解析 {@code seatDay}：{@code yyyy-MM-dd/HH:mm-HH:mm[,HH:mm-HH:mm,...]}，
 * 校验多段区间在时间上连续且对齐到配置的时间槽。
 */
@Component
public class TimeSlotCodec {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

	private final ReservationProperties props;
	private final ReservationRuleConfigService ruleConfig;

	public TimeSlotCodec(ReservationProperties props, ReservationRuleConfigService ruleConfig) {
		this.props = props;
		this.ruleConfig = ruleConfig;
	}

	public ParsedSeatDay parse(String seatDay) {
		return parse(seatDay, null);
	}

	/**
	 * @param room 可为 null；若含开放时间则预约时段须落在该窗口内；时段粒度须与全局 {@link ReservationProperties#getSlotMinutes()} 一致。
	 */
	public ParsedSeatDay parse(String seatDay, BasStudyRoomEntity room) {
		return parseSeatDay(seatDay, room, true);
	}

	/**
	 * 选座页 overlay 专用：只解析时段与槽位下标，不因「日期早于今天」「超出提前预约天数」「超过单次时长上限」拒绝。
	 * 避免服务端与客户端日期不一致、或回看历史日期时，预约合并被静默跳过导致平面图不显示已约绿格。
	 */
	public ParsedSeatDay parseForSeatBrowseOverlay(String seatDay, BasStudyRoomEntity room) {
		return parseSeatDay(seatDay, room, false);
	}

	private ParsedSeatDay parseSeatDay(String seatDay, BasStudyRoomEntity room, boolean applyBookingPolicy) {
		if (seatDay == null || seatDay.trim().isEmpty()) {
			throw new RRException("预约时间区间不能为空");
		}
		String raw = seatDay.trim();
		int slash = raw.indexOf('/');
		if (slash < 0) {
			throw new RRException("时间格式应为 yyyy-MM-dd/HH:mm-HH:mm[,HH:mm-HH:mm]");
		}
		LocalDate bizDate = LocalDate.parse(raw.substring(0, slash).trim(), DATE);
		if (applyBookingPolicy) {
			LocalDate today = LocalDate.now();
			if (bizDate.isBefore(today)) {
				throw new RRException("不能预约已过去的日期");
			}
			int maxSpanDays = ruleConfig.getAdvanceBookingDays();
			if (ChronoUnit.DAYS.between(today, bizDate) > maxSpanDays) {
				throw new RRException("仅可预约未来 " + maxSpanDays + " 天以内（含当日）的日期");
			}
		}
		String tail = raw.substring(slash + 1).trim();
		String[] segments = tail.split(",");
		LocalTime dayStart = LocalTime.parse(props.getDayStart());
		LocalTime dayEnd = LocalTime.parse(props.getDayEnd());
		int slotMins = props.getSlotMinutes();
		if (room != null && room.getSlotStepMinutes() != null) {
			slotMins = room.getSlotStepMinutes();
		}
		if (slotMins != 10 && slotMins != 30 && slotMins != 60) {
			throw new RRException("仅支持时段粒度 10、30 或 60 分钟");
		}
		if (slotMins != props.getSlotMinutes()) {
			throw new RRException("该自习室时段粒度为 " + slotMins + " 分钟，与系统配置 study.reservation.slot-minutes（"
					+ props.getSlotMinutes() + " 分钟）不一致，无法预约");
		}

		Optional<LocalTime> roomOpen = parseHm(room == null ? null : room.getOpeningTime());
		Optional<LocalTime> roomClose = parseHm(room == null ? null : room.getCloseTime());
		boolean narrowByRoom = roomOpen.isPresent() && roomClose.isPresent();
		if (narrowByRoom && !roomOpen.get().isBefore(roomClose.get())) {
			throw new RRException("自习室开放时间无效：结束须晚于开始");
		}
		if (narrowByRoom && (roomOpen.get().isBefore(dayStart) || roomClose.get().isAfter(dayEnd))) {
			throw new RRException("自习室开放时间须落在全局限定营业区间内 " + props.getDayStart() + "-" + props.getDayEnd());
		}

		List<LocalTime[]> ranges = new ArrayList<>();
		for (String seg : segments) {
			String[] ab = seg.split("-");
			if (ab.length != 2) {
				throw new RRException("时间段格式错误: " + seg);
			}
			LocalTime a = LocalTime.parse(ab[0].trim());
			LocalTime b = LocalTime.parse(ab[1].trim());
			if (!a.isBefore(b)) {
				throw new RRException("结束时间必须晚于开始时间");
			}
			if (a.isBefore(dayStart) || b.isAfter(dayEnd)) {
				throw new RRException("时间必须在营业区间内 " + props.getDayStart() + "-" + props.getDayEnd());
			}
			if (narrowByRoom && (a.isBefore(roomOpen.get()) || b.isAfter(roomClose.get()))) {
				throw new RRException("时间须在自习室开放时间内 "
						+ room.getOpeningTime() + "-" + room.getCloseTime());
			}
			if (a.getMinute() % slotMins != 0 || b.getMinute() % slotMins != 0) {
				throw new RRException("时间需按 " + slotMins + " 分钟对齐");
			}
			ranges.add(new LocalTime[]{a, b});
		}
		ranges.sort(Comparator.comparing(o -> o[0]));

		for (int i = 1; i < ranges.size(); i++) {
			LocalTime prevEnd = ranges.get(i - 1)[1];
			LocalTime curStart = ranges.get(i)[0];
			if (!prevEnd.equals(curStart)) {
				throw new RRException("多段预约必须首尾相连（连续时段）");
			}
		}

		LocalTime mergedStart = ranges.get(0)[0];
		LocalTime mergedEnd = ranges.get(ranges.size() - 1)[1];

		if (applyBookingPolicy && bizDate.equals(LocalDate.now())) {
			LocalDateTime now = LocalDateTime.now();
			LocalDateTime segmentStart = LocalDateTime.of(bizDate, mergedStart);
			LocalDateTime segmentEnd = LocalDateTime.of(bizDate, mergedEnd);
			if (!segmentStart.isAfter(now)) {
				throw new RRException("不可预约已过去的时段");
			}
			if (!segmentEnd.isAfter(now)) {
				throw new RRException("不可预约已结束的时段");
			}
		}

		int startMin = mergedStart.toSecondOfDay() / 60;
		int endMin = mergedEnd.toSecondOfDay() / 60;
		int ds = dayStart.toSecondOfDay() / 60;
		int de = dayEnd.toSecondOfDay() / 60;
		if (startMin < ds || endMin > de) {
			throw new RRException("时间超出营业范围");
		}
		if (narrowByRoom) {
			int ros = roomOpen.get().toSecondOfDay() / 60;
			int rce = roomClose.get().toSecondOfDay() / 60;
			if (startMin < ros || endMin > rce) {
				throw new RRException("时间须在自习室开放时间内 "
						+ room.getOpeningTime() + "-" + room.getCloseTime());
			}
		}
		if ((startMin - ds) % slotMins != 0 || (endMin - ds) % slotMins != 0) {
			throw new RRException("时间未对齐到 " + slotMins + " 分钟槽");
		}
		int slotStart = (startMin - ds) / slotMins;
		int slotEnd = (endMin - ds) / slotMins;
		if (slotEnd <= slotStart) {
			throw new RRException("预约时长至少一个时间槽");
		}
		int totalMinutes = (slotEnd - slotStart) * slotMins;
		if (applyBookingPolicy) {
			int minMins = props.getMinBookingMinutes();
			if (totalMinutes < minMins) {
				throw new RRException("单次预约最短时长为 " + minMins + " 分钟（" + (minMins / 60) + " 小时）");
			}
			int maxMins = ruleConfig.getMaxDurationMinutes();
			if (totalMinutes > maxMins) {
				throw new RRException("单次预约总时长不可超过 " + maxMins + " 分钟");
			}
		}
		return new ParsedSeatDay(bizDate.toString(), slotStart, slotEnd, raw);
	}

	private static Optional<LocalTime> parseHm(String s) {
		if (s == null || s.trim().isEmpty()) {
			return Optional.empty();
		}
		try {
			return Optional.of(LocalTime.parse(s.trim()));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public static final class ParsedSeatDay {
		public final String bizDate;
		public final int slotStartInclusive;
		public final int slotEndExclusive;
		public final String display;

		public ParsedSeatDay(String bizDate, int slotStartInclusive, int slotEndExclusive, String display) {
			this.bizDate = bizDate;
			this.slotStartInclusive = slotStartInclusive;
			this.slotEndExclusive = slotEndExclusive;
			this.display = display;
		}
	}
}
