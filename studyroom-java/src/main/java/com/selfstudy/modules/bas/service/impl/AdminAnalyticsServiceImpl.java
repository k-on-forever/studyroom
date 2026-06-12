package com.selfstudy.modules.bas.service.impl;

import com.selfstudy.config.ReservationProperties;
import com.selfstudy.modules.bas.dao.AdminStatsDao;
import com.selfstudy.modules.bas.dao.BasAppointmentDao;
import com.selfstudy.modules.bas.service.AdminAnalyticsService;
import com.selfstudy.modules.bas.vo.PeakSlotStatVO;
import com.selfstudy.modules.bas.vo.SeatPopularityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

	private static final DateTimeFormatter YMD = DateTimeFormatter.ISO_LOCAL_DATE;

	private final AdminStatsDao adminStatsDao;
	private final BasAppointmentDao basAppointmentDao;
	private final ReservationProperties reservationProperties;

	@Override
	public Map<String, Object> overviewForBizDate(String bizDate) {
		String d = StringUtils.hasText(bizDate) ? bizDate.trim() : LocalDate.now().format(YMD);
		Long total = adminStatsDao.countBookableSeats();
		Long booked = adminStatsDao.countDistinctBookedSeatsOn(d);
		Long inUse = adminStatsDao.countUsingSeatsNow();
		long t = total == null ? 0L : total;
		long b = booked == null ? 0L : booked;
		double rate = t <= 0 ? 0.0 : (b * 100.0 / t);
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("bizDate", d);
		m.put("totalBookableSeats", t);
		m.put("bookedSeatCount", b);
		m.put("bookingRatePercent", Math.round(rate * 100.0) / 100.0);
		m.put("inUseSeatCount", inUse == null ? 0L : inUse);
		return m;
	}

	@Override
	public Map<String, Object> todayRevenue() {
		String today = LocalDate.now().format(YMD);
		BigDecimal m = adminStatsDao.sumMembershipRevenueOn(today);
		BigDecimal a = adminStatsDao.sumAppointmentPayOn(today);
		BigDecimal total = (m == null ? BigDecimal.ZERO : m).add(a == null ? BigDecimal.ZERO : a);
		Long om = adminStatsDao.countMembershipOrdersOn(today);
		Long oa = adminStatsDao.countAppointmentPaysOn(today);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("date", today);
		row.put("membershipYuan", m == null ? BigDecimal.ZERO : m);
		row.put("appointmentYuan", a == null ? BigDecimal.ZERO : a);
		row.put("totalYuan", total);
		row.put("orderCount", (om == null ? 0L : om) + (oa == null ? 0L : oa));
		return row;
	}

	@Override
	public List<Map<String, Object>> revenueLastDays(int days) {
		int n = Math.min(90, Math.max(1, days));
		LocalDate end = LocalDate.now();
		List<Map<String, Object>> out = new ArrayList<>();
		for (int i = n - 1; i >= 0; i--) {
			LocalDate d = end.minusDays(i);
			String ds = d.format(YMD);
			out.add(dayRevenueRow(ds));
		}
		return out;
	}

	private Map<String, Object> dayRevenueRow(String ds) {
		BigDecimal m = adminStatsDao.sumMembershipRevenueOn(ds);
		BigDecimal a = adminStatsDao.sumAppointmentPayOn(ds);
		Long om = adminStatsDao.countMembershipOrdersOn(ds);
		Long oa = adminStatsDao.countAppointmentPaysOn(ds);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("date", ds);
		row.put("membershipYuan", m == null ? BigDecimal.ZERO : m);
		row.put("appointmentYuan", a == null ? BigDecimal.ZERO : a);
		row.put("totalYuan", (m == null ? BigDecimal.ZERO : m).add(a == null ? BigDecimal.ZERO : a));
		row.put("membershipOrders", om == null ? 0L : om);
		row.put("appointmentPays", oa == null ? 0L : oa);
		row.put("orderCount", (om == null ? 0L : om) + (oa == null ? 0L : oa));
		return row;
	}

	@Override
	public List<Map<String, Object>> revenueByWeeks(int pastWeeks) {
		int w = Math.min(52, Math.max(1, pastWeeks));
		LocalDate today = LocalDate.now();
		LocalDate endWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		List<Map<String, Object>> out = new ArrayList<>();
		for (int i = w - 1; i >= 0; i--) {
			LocalDate weekStart = endWeekStart.minusWeeks(i);
			LocalDate weekEnd = weekStart.plusDays(6);
			BigDecimal sumM = BigDecimal.ZERO;
			BigDecimal sumA = BigDecimal.ZERO;
			long cnt = 0;
			for (LocalDate d = weekStart; !d.isAfter(weekEnd); d = d.plusDays(1)) {
				String ds = d.format(YMD);
				Map<String, Object> row = dayRevenueRow(ds);
				sumM = sumM.add((BigDecimal) row.get("membershipYuan"));
				sumA = sumA.add((BigDecimal) row.get("appointmentYuan"));
				cnt += ((Number) row.get("orderCount")).longValue();
			}
			Map<String, Object> wrow = new LinkedHashMap<>();
			wrow.put("weekStart", weekStart.format(YMD));
			wrow.put("weekEnd", weekEnd.format(YMD));
			wrow.put("membershipYuan", sumM);
			wrow.put("appointmentYuan", sumA);
			wrow.put("totalYuan", sumM.add(sumA));
			wrow.put("orderCount", cnt);
			out.add(wrow);
		}
		return out;
	}

	@Override
	public List<Map<String, Object>> revenueByMonths(int pastMonths) {
		int mth = Math.min(24, Math.max(1, pastMonths));
		LocalDate cur = LocalDate.now().withDayOfMonth(1);
		List<Map<String, Object>> out = new ArrayList<>();
		for (int i = mth - 1; i >= 0; i--) {
			LocalDate month = cur.minusMonths(i);
			LocalDate start = month.withDayOfMonth(1);
			LocalDate end = month.with(TemporalAdjusters.lastDayOfMonth());
			BigDecimal sumM = BigDecimal.ZERO;
			BigDecimal sumA = BigDecimal.ZERO;
			long cnt = 0;
			for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
				String ds = d.format(YMD);
				Map<String, Object> row = dayRevenueRow(ds);
				sumM = sumM.add((BigDecimal) row.get("membershipYuan"));
				sumA = sumA.add((BigDecimal) row.get("appointmentYuan"));
				cnt += ((Number) row.get("orderCount")).longValue();
			}
			Map<String, Object> row = new LinkedHashMap<>();
			row.put("month", month.format(DateTimeFormatter.ofPattern("yyyy-MM")));
			row.put("membershipYuan", sumM);
			row.put("appointmentYuan", sumA);
			row.put("totalYuan", sumM.add(sumA));
			row.put("orderCount", cnt);
			out.add(row);
		}
		return out;
	}

	@Override
	public Map<String, Object> utilization(String fromDate, String toDate, int topSeats) {
		LocalDate to = StringUtils.hasText(toDate) ? LocalDate.parse(toDate.trim(), YMD) : LocalDate.now();
		LocalDate from = StringUtils.hasText(fromDate) ? LocalDate.parse(fromDate.trim(), YMD) : to.minusDays(29);
		if (from.isAfter(to)) {
			LocalDate t = from;
			from = to;
			to = t;
		}
		String fs = from.format(YMD);
		String ts = to.format(YMD);
		int lim = Math.min(50, Math.max(5, topSeats));
		List<SeatPopularityVO> hot = basAppointmentDao.statPopularSeats(fs, ts, lim);
		List<PeakSlotStatVO> peak = basAppointmentDao.statPeakSlots(fs, ts);
		enrichPeakSlotLabels(peak);
		Map<String, Object> body = new HashMap<>();
		body.put("from", fs);
		body.put("to", ts);
		body.put("popularSeats", hot);
		body.put("peakSlots", peak);
		return body;
	}

	/** 将槽位下标转成「HH:mm–HH:mm」，便于运营阅读；历史数据若为 30 分钟槽而当前为 60，会尝试兼容。非法下标不抛异常。 */
	private void enrichPeakSlotLabels(List<PeakSlotStatVO> peak) {
		if (peak == null || peak.isEmpty()) {
			return;
		}
		LocalTime dayStart = LocalTime.parse(reservationProperties.getDayStart().trim());
		int dsMin = dayStart.toSecondOfDay() / 60;
		int step = Math.max(1, reservationProperties.getSlotMinutes());
		DateTimeFormatter hm = DateTimeFormatter.ofPattern("HH:mm");
		for (PeakSlotStatVO row : peak) {
			if (row.getSlotStart() == null) {
				continue;
			}
			int idx = row.getSlotStart();
			String range = tryFormatPeakRange(dsMin, idx, step, hm);
			if (range == null && step == 60) {
				range = tryFormatPeakRange(dsMin, idx, 30, hm);
			}
			row.setTimeRange(range != null ? range : ("槽位 " + idx + "（与当前营业/槽位配置不一致）"));
		}
	}

	/**
	 * @return 可读时段，下标无法落在当日内则返回 null
	 */
	private static String tryFormatPeakRange(int dayStartMin, int slotIndex, int slotStepMinutes,
			DateTimeFormatter hm) {
		if (slotStepMinutes < 1) {
			return null;
		}
		long startMin = (long) dayStartMin + (long) slotIndex * (long) slotStepMinutes;
		if (startMin < 0 || startMin >= 24L * 60L) {
			return null;
		}
		long endMin = startMin + (long) slotStepMinutes;
		try {
			LocalTime a = LocalTime.ofSecondOfDay(startMin * 60L);
			LocalTime b;
			if (endMin >= 24L * 60L) {
				b = LocalTime.of(23, 59);
			} else {
				b = LocalTime.ofSecondOfDay(endMin * 60L);
			}
			return a.format(hm) + "–" + b.format(hm);
		} catch (Exception e) {
			return null;
		}
	}
}
