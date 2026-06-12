package com.selfstudy.modules.reservation;

import org.springframework.util.StringUtils;

/** 将 seatDay（yyyy-MM-dd/HH:mm-HH:mm）格式化为用户可读文案 */
public final class SeatDayDisplay {

	private SeatDayDisplay() {
	}

	public static String formatForMessage(String seatDay) {
		if (!StringUtils.hasText(seatDay)) {
			return "";
		}
		String raw = seatDay.trim();
		int slash = raw.indexOf('/');
		if (slash < 0) {
			return raw;
		}
		String date = raw.substring(0, slash).trim();
		String tail = raw.substring(slash + 1).trim().replace("-", " 至 ");
		return date + " " + tail;
	}

	public static String withOccupiedPrefix(String seatDay) {
		String fmt = formatForMessage(seatDay);
		if (!StringUtils.hasText(fmt)) {
			return "";
		}
		return "占用时段：" + fmt;
	}
}
