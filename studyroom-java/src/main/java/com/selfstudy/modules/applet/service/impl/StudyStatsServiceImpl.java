package com.selfstudy.modules.applet.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.selfstudy.config.ReservationProperties;
import com.selfstudy.modules.applet.dto.StudyDayCellDTO;
import com.selfstudy.modules.applet.dto.StudyStatsDTO;
import com.selfstudy.modules.applet.service.StudyStatsService;
import com.selfstudy.modules.bas.entity.BasAppointmentEntity;
import com.selfstudy.modules.bas.service.BasAppointmentService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StudyStatsServiceImpl implements StudyStatsService {

	private final BasAppointmentService appointmentService;
	private final ReservationProperties reservationProperties;

	public StudyStatsServiceImpl(BasAppointmentService appointmentService,
			ReservationProperties reservationProperties) {
		this.appointmentService = appointmentService;
		this.reservationProperties = reservationProperties;
	}

	private static LocalDate toLocalDate(java.util.Date d) {
		if (d == null) {
			return null;
		}
		return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	private int minutesFor(BasAppointmentEntity r) {
		if (r.getCheckInAt() != null && r.getStudyEndAt() != null) {
			long ms = r.getStudyEndAt().getTime() - r.getCheckInAt().getTime();
			return (int) Math.max(0L, ms / 60_000L);
		}
		if (r.getBizDate() != null && r.getSlotStart() != null && r.getSlotEnd() != null) {
			int slotM = Math.max(1, reservationProperties.getSlotMinutes());
			return Math.max(0, r.getSlotEnd() - r.getSlotStart()) * slotM;
		}
		return 0;
	}

	private LocalDate attributionDate(BasAppointmentEntity r) {
		if (r.getStudyEndAt() != null) {
			return toLocalDate(r.getStudyEndAt());
		}
		if (r.getBizDate() != null && !r.getBizDate().isBlank()) {
			return LocalDate.parse(r.getBizDate().trim());
		}
		return null;
	}

	private static int levelFromMinutes(int minutes) {
		if (minutes <= 0) {
			return 0;
		}
		if (minutes <= 30) {
			return 1;
		}
		if (minutes <= 90) {
			return 2;
		}
		if (minutes <= 180) {
			return 3;
		}
		return 4;
	}

	@Override
	public StudyStatsDTO statsForUser(Long userId) {
		LambdaQueryWrapper<BasAppointmentEntity> w = new LambdaQueryWrapper<>();
		w.eq(BasAppointmentEntity::getUserId, userId).eq(BasAppointmentEntity::getSeatState, 3);
		List<BasAppointmentEntity> done = appointmentService.list(w);

		Map<LocalDate, Integer> minutesByDay = new HashMap<>();
		long total = 0L;
		for (BasAppointmentEntity r : done) {
			int m = minutesFor(r);
			total += m;
			LocalDate d = attributionDate(r);
			if (d != null) {
				minutesByDay.merge(d, m, Integer::sum);
			}
		}

		LocalDate today = LocalDate.now();
		LocalDate start = today.minusDays(83);
		List<StudyDayCellDTO> heat = new ArrayList<>(84);
		for (int i = 0; i < 84; i++) {
			LocalDate d = start.plusDays(i);
			int mins = minutesByDay.getOrDefault(d, 0);
			StudyDayCellDTO c = new StudyDayCellDTO();
			c.setDate(d.toString());
			c.setMinutes(mins);
			c.setLevel(levelFromMinutes(mins));
			heat.add(c);
		}

		LocalDate streakEnd = null;
		if (minutesByDay.containsKey(today)) {
			streakEnd = today;
		} else if (minutesByDay.containsKey(today.minusDays(1))) {
			streakEnd = today.minusDays(1);
		}
		int streak = 0;
		if (streakEnd != null) {
			for (LocalDate x = streakEnd; minutesByDay.containsKey(x); x = x.minusDays(1)) {
				streak++;
			}
		}

		StudyStatsDTO out = new StudyStatsDTO();
		out.setTotalMinutes(total);
		out.setStreakDays(streak);
		out.setHeatmapDays(heat);
		return out;
	}
}
